# Course Prerequisite Tracker

A small web app that answers two questions for a course catalog:

1. **"To take this course, what do I need to complete first?"** — the full prerequisite chain, however many hops deep.
2. **"Given what I've already finished, what can I take right now?"** — eligibility, computed from every prerequisite being satisfied.

Backed by **CognoDB**, a managed graph database, via the official Neo4j Java driver.

---

## Why a graph database?

A course catalog *looks* like a table (`courses`, `students`, `enrollments`) until you ask a question that involves **chains of relationships** — and then a relational schema starts fighting you:

- **"What's the full prerequisite chain for Advanced ML?"** In SQL this needs a recursive CTE (`WITH RECURSIVE`) that self-joins a `prerequisites` table an unknown number of times. In Cypher it's one pattern: `(target)-[:REQUIRES*1..6]->(prereq)`. The traversal depth isn't fixed, because different courses sit at different depths in the curriculum — a graph handles "however many hops it takes" natively.
- **"What can this student take right now?"** This requires checking that *every* prerequisite of *every* candidate course is in the student's completed set — a conjunction of negations, per course. In SQL that's a `NOT EXISTS` correlated subquery nested inside another `NOT EXISTS`, re-evaluated per candidate course. In Cypher it reads the same way you'd say it out loud: a course qualifies unless there exists any unmet prerequisite.
- **Cross-department prerequisites** (e.g. `DS301 Machine Learning` requires both `MATH210 Probability & Statistics` and `CS102 Data Structures`) are just edges — no join table bookkeeping, no schema change if a new kind of relationship (like `RECOMMENDED_BEFORE`) shows up later.

None of this is impossible in SQL — it's just that the *interesting* queries here are fundamentally about **variable-depth connectivity**, which is what graph databases are built for. A relational schema would work, but every one of the queries above would need noticeably more SQL and would read less like the question being asked.

---

## Data model

**Nodes**

| Label | Key properties |
|---|---|
| `Course` | `code`, `title`, `department`, `credits`, `description` |
| `Student` | `id`, `name`, `year` |
| `Department` | `name` |

**Relationships**

| Relationship | Direction | Meaning |
|---|---|---|
| `(Course)-[:REQUIRES]->(Course)` | course → its prerequisite | mandatory prerequisite chain (the multi-hop edge) |
| `(Student)-[:COMPLETED {grade, semester}]->(Course)` | student → course | a finished course, with properties on the edge |
| `(Course)-[:BELONGS_TO]->(Department)` | course → department | catalog grouping |

```
                     ┌──────────────┐
                     │  Department  │
                     └──────▲───────┘
                            │ BELONGS_TO
                            │
   REQUIRES        ┌───────┴───────┐        REQUIRES
 ┌─────────────────│    Course     │─────────────────┐
 │                  └───────────────┘                 │
 ▼                                                     ▼
Course ◄──── REQUIRES ──── Course ◄──── REQUIRES ──── Course
   ▲
   │ COMPLETED {grade, semester}
   │
┌──┴─────┐
│ Student │
└─────────┘
```

Example chain in the seed data:
`DS401 Advanced ML → DS301 Machine Learning → DS201 Statistics for DS → MATH210 Probability & Statistics → MATH102 Calculus II → MATH101 Calculus I` (5 hops).

---

## Key queries

All queries are parameterized (`$code`, `$studentId`, …) via the Neo4j driver — never string-concatenated — to avoid Cypher injection. See `backend/src/main/java/com/wexa/prereq/repository/`.

**1. Multi-hop prerequisite chain** (`CourseRepository.findPrerequisiteChain`)
```cypher
MATCH path = (target:Course {code: $code})-[:REQUIRES*1..6]->(prereq:Course)
WITH prereq, min(length(path)) AS depth
RETURN prereq.code AS code, prereq.title AS title, prereq.department AS department, depth
ORDER BY depth, prereq.code
```
Follows `REQUIRES` edges 1 to 6 hops deep and returns every ancestor with how many hops away it is — this is what draws the "ladder" on the course detail page.

**2. Eligible courses for a student** (`CourseRepository.findEligibleCourses`) — the relational-awkward one
```cypher
MATCH (c:Course)
WHERE NOT EXISTS {
    MATCH (s:Student {id: $studentId})-[:COMPLETED]->(c)
}
AND NOT EXISTS {
    MATCH (c)-[:REQUIRES]->(req:Course)
    WHERE NOT EXISTS {
        MATCH (s2:Student {id: $studentId})-[:COMPLETED]->(req)
    }
}
RETURN c.code AS code, c.title AS title, c.department AS department, c.credits AS credits, c.description AS description
ORDER BY c.department, c.code
```
A course is eligible if it isn't already completed, and there is no prerequisite of it that the student *hasn't* completed.

**3. Shortest path between two courses** (`CourseRepository.shortestPath`)
```cypher
MATCH path = shortestPath((a:Course {code: $from})-[:REQUIRES*]-(b:Course {code: $to}))
RETURN [n IN nodes(path) | n.code] AS codes
```

**4. What a course unlocks** (`CourseRepository.findCoursesUnlockedBy`) — reverse 1-hop traversal, shown on the course detail page.

---

## Project structure

```
course-prereq-tracker/
├── backend/                      Spring Boot REST API
│   └── src/main/java/com/wexa/prereq/
│       ├── config/                Neo4j driver bean, CORS
│       ├── model/                 Course, Student
│       ├── repository/            All Cypher queries live here
│       ├── controller/            REST endpoints + global error handling
│       └── SeedDataLoader.java    Loads seed/seed.cypher into CognoDB
├── frontend/                     React app
│   └── src/
│       ├── api.js                 Fetch wrapper with friendly error messages
│       ├── pages/                 Home (catalog), CourseDetail, StudentDashboard
│       └── components/            Loading / empty / error states
├── seed/
│   └── seed.cypher                Sample courses, departments, students, edges
├── .env.example
└── README.md
```

---

## Setup & run

### 1. Create your CognoDB instance
1. Sign up at [console.cognodb.com/signup](https://console.cognodb.com/signup) (no credit card).
2. Create a free (`c0`) instance and pick a region — provisioning takes under a minute.
3. Copy the connection URI (`bolt+s://<instance-id>.databases.cognodb.cloud`) and the generated password for user `cognodb`. **The password is shown only once** — save it now.

### 2. Configure environment variables
```bash
cp .env.example backend/.env   # or export the variables directly in your shell
```
Fill in `COGNODB_URI`, `COGNODB_USERNAME` (`cognodb`), `COGNODB_PASSWORD`.

### 3. Load seed data
```bash
cd backend
export COGNODB_URI=bolt+s://your-instance-id.databases.cognodb.cloud
export COGNODB_USERNAME=cognodb
export COGNODB_PASSWORD=your-password
mvn compile exec:java -Dexec.mainClass=com.wexa.prereq.SeedDataLoader
```
This loads ~17 courses across 4 departments, their prerequisite chains, and 3 sample students with completed courses.

(Alternative: paste the contents of `seed/seed.cypher` directly into the CognoDB Cloud console's query editor.)

### 4. Run the backend
```bash
cd backend
export COGNODB_URI=... COGNODB_USERNAME=cognodb COGNODB_PASSWORD=...
mvn spring-boot:run
```
API is available at `http://localhost:8080/api`.

### 5. Run the frontend
```bash
cd frontend
npm install
npm start
```
App opens at `http://localhost:3000`.

---

## Error handling

If CognoDB is unreachable, the backend's `GlobalExceptionHandler` catches `ServiceUnavailableException` and returns a clean `503` JSON response instead of a stack trace. The frontend's `api.js` wraps every fetch call and surfaces failures as a dedicated `ErrorState` component with a retry button, rather than a blank page or an uncaught exception.

---



