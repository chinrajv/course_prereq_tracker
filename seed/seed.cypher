// ============================================================================
// Course Prerequisite Tracker — Seed Data
// Run this against your CognoDB instance to load sample courses, students,
// and their relationships. You can paste this into the CognoDB Cloud
// console's query editor, or run it via cypher-shell / the driver.
// ============================================================================

// --- Clean slate (safe to re-run) -------------------------------------------
MATCH (n) DETACH DELETE n;

// --- Departments --------------------------------------------------------------
CREATE (:Department {name: 'Computer Science'});
CREATE (:Department {name: 'Mathematics'});
CREATE (:Department {name: 'Physics'});
CREATE (:Department {name: 'Data Science'});

// --- Courses --------------------------------------------------------------
CREATE (:Course {code: 'MATH101', title: 'Calculus I', department: 'Mathematics', credits: 4,
  description: 'Limits, derivatives, and an introduction to integration.'});
CREATE (:Course {code: 'MATH102', title: 'Calculus II', department: 'Mathematics', credits: 4,
  description: 'Integration techniques, series, and an introduction to multivariable calculus.'});
CREATE (:Course {code: 'MATH201', title: 'Linear Algebra', department: 'Mathematics', credits: 3,
  description: 'Vector spaces, matrices, eigenvalues, and linear transformations.'});
CREATE (:Course {code: 'MATH210', title: 'Probability & Statistics', department: 'Mathematics', credits: 3,
  description: 'Probability theory, distributions, hypothesis testing.'});

CREATE (:Course {code: 'CS101', title: 'Introduction to Programming', department: 'Computer Science', credits: 4,
  description: 'Fundamentals of programming using Python: variables, control flow, functions.'});
CREATE (:Course {code: 'CS102', title: 'Data Structures', department: 'Computer Science', credits: 4,
  description: 'Arrays, linked lists, trees, hash maps, and complexity analysis.'});
CREATE (:Course {code: 'CS201', title: 'Algorithms', department: 'Computer Science', credits: 4,
  description: 'Algorithm design paradigms: greedy, divide & conquer, dynamic programming.'});
CREATE (:Course {code: 'CS210', title: 'Discrete Mathematics', department: 'Computer Science', credits: 3,
  description: 'Logic, set theory, combinatorics, and graph theory for computer science.'});
CREATE (:Course {code: 'CS301', title: 'Databases', department: 'Computer Science', credits: 3,
  description: 'Relational and non-relational data modeling, SQL, transactions.'});
CREATE (:Course {code: 'CS310', title: 'Operating Systems', department: 'Computer Science', credits: 4,
  description: 'Processes, threads, memory management, scheduling, file systems.'});
CREATE (:Course {code: 'CS320', title: 'Computer Networks', department: 'Computer Science', credits: 3,
  description: 'Network protocols, the OSI model, routing, and network security basics.'});

CREATE (:Course {code: 'DS201', title: 'Statistics for Data Science', department: 'Data Science', credits: 3,
  description: 'Applied statistics and experiment design for data-driven decision making.'});
CREATE (:Course {code: 'DS301', title: 'Machine Learning', department: 'Data Science', credits: 4,
  description: 'Supervised and unsupervised learning, model evaluation, feature engineering.'});
CREATE (:Course {code: 'DS401', title: 'Advanced Machine Learning', department: 'Data Science', credits: 4,
  description: 'Deep learning, neural network architectures, and modern ML systems.'});
CREATE (:Course {code: 'DS410', title: 'Natural Language Processing', department: 'Data Science', credits: 3,
  description: 'Text processing, embeddings, sequence models, and transformers.'});

CREATE (:Course {code: 'PHY101', title: 'Physics I: Mechanics', department: 'Physics', credits: 4,
  description: 'Kinematics, Newtonian mechanics, energy, and momentum.'});
CREATE (:Course {code: 'PHY201', title: 'Physics II: Electromagnetism', department: 'Physics', credits: 4,
  description: 'Electric and magnetic fields, circuits, and Maxwell\'s equations.'});

// --- Link courses to departments -------------------------------------------
MATCH (c:Course), (d:Department) WHERE c.department = d.name
CREATE (c)-[:BELONGS_TO]->(d);

// --- Prerequisite chains (REQUIRES) ----------------------------------------
// Math track
MATCH (a:Course {code: 'MATH102'}), (b:Course {code: 'MATH101'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'MATH201'}), (b:Course {code: 'MATH102'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'MATH210'}), (b:Course {code: 'MATH102'}) CREATE (a)-[:REQUIRES]->(b);

// CS core track
MATCH (a:Course {code: 'CS102'}), (b:Course {code: 'CS101'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS201'}), (b:Course {code: 'CS102'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS201'}), (b:Course {code: 'CS210'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS210'}), (b:Course {code: 'CS101'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS301'}), (b:Course {code: 'CS102'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS310'}), (b:Course {code: 'CS201'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'CS320'}), (b:Course {code: 'CS310'}) CREATE (a)-[:REQUIRES]->(b);

// Data Science track (crosses department boundaries - a good graph story)
MATCH (a:Course {code: 'DS201'}), (b:Course {code: 'MATH210'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'DS301'}), (b:Course {code: 'DS201'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'DS301'}), (b:Course {code: 'MATH201'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'DS301'}), (b:Course {code: 'CS102'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'DS401'}), (b:Course {code: 'DS301'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'DS410'}), (b:Course {code: 'DS301'}) CREATE (a)-[:REQUIRES]->(b);

// Physics track
MATCH (a:Course {code: 'PHY201'}), (b:Course {code: 'PHY101'}) CREATE (a)-[:REQUIRES]->(b);
MATCH (a:Course {code: 'PHY101'}), (b:Course {code: 'MATH101'}) CREATE (a)-[:REQUIRES]->(b);

// --- Students ---------------------------------------------------------------
CREATE (:Student {id: 'S001', name: 'Ananya Rao', year: '3rd Year'});
CREATE (:Student {id: 'S002', name: 'Kabir Menon', year: '1st Year'});
CREATE (:Student {id: 'S003', name: 'Priya Suresh', year: '4th Year'});

// --- Completed courses (COMPLETED relationship with properties) -----------
MATCH (s:Student {id: 'S001'}), (c:Course {code: 'MATH101'}) CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Fall 2023'}]->(c);
MATCH (s:Student {id: 'S001'}), (c:Course {code: 'MATH102'}) CREATE (s)-[:COMPLETED {grade: 'A-', semester: 'Spring 2024'}]->(c);
MATCH (s:Student {id: 'S001'}), (c:Course {code: 'CS101'})   CREATE (s)-[:COMPLETED {grade: 'B+', semester: 'Fall 2023'}]->(c);
MATCH (s:Student {id: 'S001'}), (c:Course {code: 'CS102'})   CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Spring 2024'}]->(c);
MATCH (s:Student {id: 'S001'}), (c:Course {code: 'MATH210'}) CREATE (s)-[:COMPLETED {grade: 'B', semester: 'Fall 2024'}]->(c);

MATCH (s:Student {id: 'S002'}), (c:Course {code: 'MATH101'}) CREATE (s)-[:COMPLETED {grade: 'B', semester: 'Fall 2024'}]->(c);
MATCH (s:Student {id: 'S002'}), (c:Course {code: 'CS101'})   CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Fall 2024'}]->(c);

MATCH (s:Student {id: 'S003'}), (c:Course {code: 'MATH101'}) CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Fall 2021'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'MATH102'}) CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Spring 2022'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'MATH201'}) CREATE (s)-[:COMPLETED {grade: 'A-', semester: 'Fall 2022'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'MATH210'}) CREATE (s)-[:COMPLETED {grade: 'B+', semester: 'Fall 2022'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'CS101'})   CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Fall 2021'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'CS102'})   CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Spring 2022'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'DS201'})   CREATE (s)-[:COMPLETED {grade: 'A', semester: 'Fall 2022'}]->(c);
MATCH (s:Student {id: 'S003'}), (c:Course {code: 'DS301'})   CREATE (s)-[:COMPLETED {grade: 'A-', semester: 'Spring 2023'}]->(c);
