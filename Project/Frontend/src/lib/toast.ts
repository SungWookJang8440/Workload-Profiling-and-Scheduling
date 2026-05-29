import { useEffect, useState } from 'react';

interface Toast {
  id: string;
  title: string;
  description?: string;
  variant?: 'default' | 'destructive' | 'success';
}

let toasts: Toast[] = [];
let listeners: ((toasts: Toast[]) => void)[] = [];

const notifyListeners = () => {
  listeners.forEach(listener => listener([...toasts]));
};

const removeToast = (id: string) => {
  toasts = toasts.filter(t => t.id !== id);
  notifyListeners();
};

export const toast = {
  add: (toastItem: Omit<Toast, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    toasts = [...toasts, { ...toastItem, id }];
    notifyListeners();
    
    // Auto remove after 5 seconds
    setTimeout(() => {
      removeToast(id);
    }, 5000);
    
    return id;
  },
  
  remove: removeToast,
  
  success: (title: string, description?: string) => {
    return toast.add({ title, description, variant: 'success' });
  },
  
  error: (title: string, description?: string) => {
    return toast.add({ title, description, variant: 'destructive' });
  },
  
  subscribe: (listener: (toasts: Toast[]) => void) => {
    listeners.push(listener);
    return () => {
      listeners = listeners.filter(l => l !== listener);
    };
  }
};

export function useToasts() {
  const [currentToasts, setCurrentToasts] = useState<Toast[]>([]);
  
  useEffect(() => {
    return toast.subscribe(setCurrentToasts);
  }, []);
  
  return currentToasts;
}
