import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { LoadingState, EmptyState, ErrorState } from '../components/StatusStates';
import './StudentDashboard.css';

export default function StudentDashboard() {
  const [students, setStudents] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [completed, setCompleted] = useState(null);
  const [eligible, setEligible] = useState(null);
  const [error, setError] = useState(null);
  const [marking, setMarking] = useState(null);

  useEffect(() => {
    api.getStudents()
      .then((data) => {
        setStudents(data);
        if (data.length > 0) setSelectedId(data[0].id);
      })
      .catch((e) => setError(e.message));
  }, []);

  const loadStudentData = (id) => {
    setError(null);
    setCompleted(null);
    setEligible(null);
    Promise.all([api.getCompletedCourses(id), api.getEligibleCourses(id)])
      .then(([c, e]) => {
        setCompleted(c);
        setEligible(e);
      })
      .catch((e) => setError(e.message));
  };

  useEffect(() => {
    if (selectedId) loadStudentData(selectedId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId]);

  const handleMarkComplete = async (courseCode) => {
    setMarking(courseCode);
    try {
      await api.completeCourse(selectedId, courseCode, '', '');
      loadStudentData(selectedId);
    } catch (e) {
      setError(e.message);
    } finally {
      setMarking(null);
    }
  };

  if (error && !students) return <ErrorState message={error} onRetry={() => window.location.reload()} />;
  if (!students) return <LoadingState label="Loading students…" />;

  const currentStudent = students.find((s) => s.id === selectedId);

  return (
    <div>
      <div className="page-title">
        <div>
          <h1>Student Progress</h1>
          <p>Pick a student to see what they've finished and what opens up next.</p>
        </div>
      </div>

      {students.length === 0 ? (
        <EmptyState title="No students yet" hint="Run the seed script to load sample students." />
      ) : (
        <>
          <div className="student-picker">
            {students.map((s) => (
              <button
                key={s.id}
                className={`student-tab ${selectedId === s.id ? 'student-tab--active' : ''}`}
                onClick={() => setSelectedId(s.id)}
              >
                {s.name}
                <span>{s.year}</span>
              </button>
            ))}
          </div>

          {error && <ErrorState message={error} onRetry={() => loadStudentData(selectedId)} />}

          {!error && (!completed || !eligible) && <LoadingState label="Loading progress…" />}

          {!error && completed && eligible && (
            <div className="dashboard-grid">
              <section>
                <h2>Completed ({completed.length})</h2>
                {completed.length === 0 ? (
                  <EmptyState
                    title="Nothing completed yet"
                    hint={`${currentStudent?.name} hasn't finished any courses.`}
                  />
                ) : (
                  <ul className="completed-list">
                    {completed.map((c) => (
                      <li key={c.code}>
                        <Link to={`/courses/${c.code}`}>
                          <span className="mono">{c.code}</span> {c.title}
                        </Link>
                        <span className="completed-list__meta">
                          {c.grade && <span className="grade-badge">{c.grade}</span>}
                          {c.semester}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section>
                <h2>Eligible now ({eligible.length})</h2>
                <p className="section-hint">
                  Every prerequisite is satisfied — {currentStudent?.name} can take these next.
                </p>
                {eligible.length === 0 ? (
                  <EmptyState
                    title="Nothing eligible right now"
                    hint="Complete more prerequisites to unlock new courses."
                  />
                ) : (
                  <ul className="eligible-list">
                    {eligible.map((c) => (
                      <li key={c.code}>
                        <Link to={`/courses/${c.code}`}>
                          <span className="mono">{c.code}</span> {c.title}
                        </Link>
                        <button
                          className="btn btn--outline btn--small"
                          disabled={marking === c.code}
                          onClick={() => handleMarkComplete(c.code)}
                        >
                          {marking === c.code ? 'Saving…' : 'Mark complete'}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>
          )}
        </>
      )}
    </div>
  );
}
