import React from 'react';
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import Home from './pages/Home';
import CourseDetail from './pages/CourseDetail';
import StudentDashboard from './pages/StudentDashboard';
import './App.css';

export default function App() {
  return (
    <BrowserRouter>
      <header className="app-header">
        <div className="app-header__inner">
          <NavLink to="/" className="app-header__brand">
            <span className="app-header__mark">§</span>
            Course Ledger
          </NavLink>
          <nav className="app-header__nav">
            <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>
              Catalog
            </NavLink>
            <NavLink to="/students" className={({ isActive }) => isActive ? 'active' : ''}>
              Student Progress
            </NavLink>
          </nav>
        </div>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/courses/:code" element={<CourseDetail />} />
          <Route path="/students" element={<StudentDashboard />} />
        </Routes>
      </main>

      <footer className="app-footer">
        Course Prerequisite Tracker — backed by CognoDB (graph database)
      </footer>
    </BrowserRouter>
  );
}
