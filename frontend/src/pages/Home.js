import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { LoadingState, EmptyState, ErrorState } from '../components/StatusStates';
import './Home.css';

export default function Home() {
  const [courses, setCourses] = useState(null);
  const [error, setError] = useState(null);
  const [department, setDepartment] = useState('All');
  const [query, setQuery] = useState('');

  const load = () => {
    setError(null);
    setCourses(null);
    api.getCourses().then(setCourses).catch((e) => setError(e.message));
  };

  useEffect(load, []);

  const departments = useMemo(() => {
    if (!courses) return [];
    return ['All', ...new Set(courses.map((c) => c.department))];
  }, [courses]);

  const filtered = useMemo(() => {
    if (!courses) return [];
    return courses.filter((c) => {
      const matchesDept = department === 'All' || c.department === department;
      const matchesQuery =
        query.trim() === '' ||
        c.title.toLowerCase().includes(query.toLowerCase()) ||
        c.code.toLowerCase().includes(query.toLowerCase());
      return matchesDept && matchesQuery;
    });
  }, [courses, department, query]);

  return (
    <div>
      <div className="page-title">
        <div>
          <h1>Course Catalog</h1>
          <p>Browse every course and trace what leads to what.</p>
        </div>
      </div>

      {courses && courses.length > 0 && (
        <div className="catalog-controls">
          <input
            className="catalog-search"
            type="search"
            placeholder="Search by title or code…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="catalog-filters">
            {departments.map((d) => (
              <button
                key={d}
                className={`chip ${department === d ? 'chip--active' : ''}`}
                onClick={() => setDepartment(d)}
              >
                {d}
              </button>
            ))}
          </div>
        </div>
      )}

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !courses && <LoadingState label="Loading catalog…" />}
      {!error && courses && courses.length === 0 && (
        <EmptyState
          title="No courses yet"
          hint="Run the seed script to load sample data into CognoDB."
        />
      )}
      {!error && courses && courses.length > 0 && filtered.length === 0 && (
        <EmptyState title="No matches" hint="Try a different search or department." />
      )}

      {filtered.length > 0 && (
        <div className="course-grid">
          {filtered.map((c) => (
            <Link to={`/courses/${c.code}`} key={c.code} className="course-card">
              <div className="course-card__code mono">{c.code}</div>
              <h3>{c.title}</h3>
              <p>{c.description}</p>
              <div className="course-card__meta">
                <span>{c.department}</span>
                <span>{c.credits} credits</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
