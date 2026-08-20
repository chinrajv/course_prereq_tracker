const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Thin fetch wrapper. Throws an Error with a friendly message on failure so
 * calling components can render a clean error state instead of a raw
 * network exception - this is what lets the UI degrade gracefully when
 * CognoDB (or the backend) is unreachable.
 */
async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch (networkError) {
    throw new Error('Could not reach the server. Is the backend running?');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch {
      // ignore - use default message
    }
    throw new Error(message);
  }

 if (response.status === 204) return null;

  const text = await response.text();
  if (!text) return null; // empty body - nothing to parse
  return JSON.parse(text);
}

export const api = {
  getCourses: () => request('/courses'),
  getCourse: (code) => request(`/courses/${code}`),
  getPrerequisiteChain: (code) => request(`/courses/${code}/prerequisites/chain`),
  getDirectPrerequisites: (code) => request(`/courses/${code}/prerequisites/direct`),
  getUnlockedCourses: (code) => request(`/courses/${code}/unlocks`),
  getShortestPath: (from, to) => request(`/courses/path?from=${from}&to=${to}`),

  getStudents: () => request('/students'),
  getCompletedCourses: (id) => request(`/students/${id}/completed`),
  getEligibleCourses: (id) => request(`/students/${id}/eligible-courses`),
  completeCourse: (id, courseCode, grade, semester) =>
    request(`/students/${id}/complete/${courseCode}`, {
      method: 'POST',
      body: JSON.stringify({ grade, semester }),
    }),
};
