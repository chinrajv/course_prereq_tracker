import React, { useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api';
import { LoadingState, EmptyState, ErrorState } from '../components/StatusStates';
import './CourseDetail.css';

export default function CourseDetail() {
  const { code } = useParams();
  const [course, setCourse] = useState(null);
  const [chain, setChain] = useState(null);
  const [unlocks, setUnlocks] = useState(null);
  const [error, setError] = useState(null);

  const load = () => {
    setError(null);
    setCourse(null);
    setChain(null);
    setUnlocks(null);
    Promise.all([
      api.getCourse(code),
      api.getPrerequisiteChain(code),
      api.getUnlockedCourses(code),
    ])
      .then(([c, chainData, unlockData]) => {
        setCourse(c);
        setChain(chainData);
        setUnlocks(unlockData);
      })
      .catch((e) => setError(e.message));
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [code]);

  // Group the flat prerequisite-chain result by depth, so it renders as a
  // ladder: the target course at the top, its direct prerequisites on the
  // first rung, their prerequisites on the next rung down, etc.
  const rungs = useMemo(() => {
    if (!chain) return [];
    const byDepth = {};
    chain.forEach((item) => {
      byDepth[item.depth] = byDepth[item.depth] || [];
      byDepth[item.depth].push(item);
    });
    return Object.keys(byDepth)
      .sort((a, b) => Number(a) - Number(b))
      .map((depth) => ({ depth: Number(depth), items: byDepth[depth] }));
  }, [chain]);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!course) return <LoadingState label="Loading course…" />;

  return (
    <div>
      <Link to="/" className="back-link">← Back to catalog</Link>

      <div className="course-header">
        <div className="course-header__code mono">{course.code}</div>
        <h1>{course.title}</h1>
        <p className="course-header__meta">
          {course.department} · {course.credits} credits
        </p>
        <p className="course-header__desc">{course.description}</p>
      </div>

      <section className="ladder-section">
        <h2>Prerequisite chain</h2>
        <p className="section-hint">
          Everything you need to complete, traced back rung by rung, before you can take {course.code}.
        </p>

        {rungs.length === 0 ? (
          <EmptyState
            title="No prerequisites"
            hint={`${course.code} has no prerequisites — it's an entry point into its track.`}
          />
        ) : (
          <div className="ladder">
            <div className="ladder__rung ladder__rung--target">
              <div className="ladder__spine-dot ladder__spine-dot--target" />
              <div className="ladder__node ladder__node--target">
                <span className="mono">{course.code}</span> {course.title}
              </div>
            </div>

            {rungs.map((rung) => (
              <div className="ladder__rung" key={rung.depth}>
                <div className="ladder__spine-dot" />
                <div className="ladder__rung-label">
                  {rung.depth} hop{rung.depth > 1 ? 's' : ''} back
                </div>
                <div className="ladder__nodes">
                  {rung.items.map((item) => (
                    <Link to={`/courses/${item.code}`} className="ladder__node" key={item.code}>
                      <span className="mono">{item.code}</span> {item.title}
                    </Link>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="unlocks-section">
        <h2>Unlocks</h2>
        <p className="section-hint">Courses that require {course.code} as a prerequisite.</p>
        {!unlocks || unlocks.length === 0 ? (
          <EmptyState title="Nothing depends on this course yet" />
        ) : (
          <div className="unlock-list">
            {unlocks.map((u) => (
              <Link to={`/courses/${u.code}`} key={u.code} className="unlock-pill">
                <span className="mono">{u.code}</span> {u.title}
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
