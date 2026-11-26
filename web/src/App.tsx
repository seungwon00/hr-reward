import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import LoginPage from '@/pages/LoginPage';
import SignUpPage from '@/pages/SignUpPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
