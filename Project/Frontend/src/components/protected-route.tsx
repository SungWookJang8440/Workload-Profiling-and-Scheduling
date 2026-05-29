import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/lib/store';

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, fetchCurrentUser } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) {
      fetchCurrentUser();
    }
  }, [isAuthenticated, fetchCurrentUser]);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
