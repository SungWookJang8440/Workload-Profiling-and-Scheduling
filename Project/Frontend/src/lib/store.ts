import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api from './api';

export interface User {
  id: number;
  username: string;
  email: string;
  is_admin: boolean;
}

export interface Container {
  id: number;
  user_id: number;
  cluster_id: number;
  container_id: string;
  image_name: string;
  ssh_port_mapped: number;
  jupyter_port_mapped: number | null;
  ssh_command: string;
  ssh_password: string;
  status: 'STARTING' | 'RUNNING' | 'STOPPED' | 'ERROR';
  started_at: string;
  stopped_at: string | null;
}

export interface Cluster {
  id: number;
  name: string;
  ip_address: string;
  ssh_port: number;
  gpu_name: string;
  gpu_count: number;
  gpu_vram_gb: number;
  specs: Record<string, any>;
  is_active: boolean;
  status: string;
  created_at: string;
}

export interface Template {
  id: number;
  image_name: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // Actions
  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => void;
  fetchCurrentUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      setAuth: (user, token) => {
        localStorage.setItem('token', token);
        set({ user, token, isAuthenticated: true });
      },

      clearAuth: () => {
        localStorage.removeItem('token');
        set({ user: null, token: null, isAuthenticated: false });
      },

      login: async (email, password) => {
        set({ isLoading: true });
        try {
          const response = await api.login(email, password);
          const { accessToken, user } = response;
          get().setAuth(user, accessToken);
        } finally {
          set({ isLoading: false });
        }
      },

      register: async (email, password, name) => {
        set({ isLoading: true });
        try {
          const response = await api.register(email, password, name);
          const { accessToken, user } = response;
          get().setAuth(user, accessToken);
        } finally {
          set({ isLoading: false });
        }
      },

      logout: () => {
        get().clearAuth();
      },

      fetchCurrentUser: async () => {
        try {
          const response = await api.getCurrentUser();
          set({ user: response.user });
        } catch (error) {
          get().clearAuth();
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ token: state.token }),
    }
  )
);

interface ContainerState {
  containers: Container[];
  isLoading: boolean;
  
  fetchContainers: () => Promise<void>;
  createContainer: (imageName: string) => Promise<void>;
  deleteContainer: (containerId: string) => Promise<void>;
  reconcileSessions: () => Promise<void>;
}

export const useContainerStore = create<ContainerState>((set, get) => ({
  containers: [],
  isLoading: false,

  fetchContainers: async () => {
    set({ isLoading: true });
    try {
      const response = await api.getContainers();
      set({ containers: response.data || [] });
    } finally {
      set({ isLoading: false });
    }
  },

  createContainer: async (imageName) => {
    set({ isLoading: true });
    try {
      await api.createContainer(imageName);
      await get().fetchContainers();
    } finally {
      set({ isLoading: false });
    }
  },

  deleteContainer: async (containerId) => {
    set({ isLoading: true });
    try {
      await api.deleteContainer(containerId);
      await get().fetchContainers();
    } finally {
      set({ isLoading: false });
    }
  },

  reconcileSessions: async () => {
    await api.reconcileSessions();
    await get().fetchContainers();
  },
}));

interface ClusterState {
  clusters: Cluster[];
  isLoading: boolean;
  
  fetchClusters: () => Promise<void>;
  addCluster: (machineName: string, ipAddress?: string, description?: string) => Promise<void>;
}

export const useClusterStore = create<ClusterState>((set, get) => ({
  clusters: [],
  isLoading: false,

  fetchClusters: async () => {
    set({ isLoading: true });
    try {
      const response = await api.getClusters();
      set({ clusters: response.clusters || [] });
    } finally {
      set({ isLoading: false });
    }
  },

  addCluster: async (machineName, ipAddress, description) => {
    set({ isLoading: true });
    try {
      await api.addCluster(machineName, ipAddress, description);
      await get().fetchClusters();
    } finally {
      set({ isLoading: false });
    }
  },
}));

interface TemplateState {
  templates: Template[];
  isLoading: boolean;
  
  fetchTemplates: () => Promise<void>;
  addTemplate: (imageName: string) => Promise<void>;
}

export const useTemplateStore = create<TemplateState>((set, get) => ({
  templates: [],
  isLoading: false,

  fetchTemplates: async () => {
    set({ isLoading: true });
    try {
      const response = await api.getTemplates();
      set({ templates: response.templates || [] });
    } finally {
      set({ isLoading: false });
    }
  },

  addTemplate: async (imageName) => {
    set({ isLoading: true });
    try {
      await api.addTemplate(imageName);
      await get().fetchTemplates();
    } finally {
      set({ isLoading: false });
    }
  },
}));
