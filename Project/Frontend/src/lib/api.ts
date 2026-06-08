import axios, { AxiosInstance, AxiosError } from 'axios';

const API_BASE_URL = (import.meta as any).env?.VITE_API_URL || '/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth APIs
  async register(email: string, password: string, name: string) {
    const response = await this.client.post('/auth/register', { email, password, name });
    return response.data;
  }

  async login(email: string, password: string) {
    const response = await this.client.post('/auth/login', { email, password });
    return response.data;
  }

  async getCurrentUser() {
    const response = await this.client.get('/auth/me');
    return response.data;
  }

  async changePassword(currentPassword: string, newPassword: string) {
    const response = await this.client.post('/auth/change-password', {
      current_password: currentPassword,
      new_password: newPassword,
    });
    return response.data;
  }

  // Container APIs
  async getContainers() {
    const response = await this.client.get('/get_containers');
    return response.data;
  }

  async getContainer(containerId: string) {
    const response = await this.client.get(`/get_container/${containerId}`);
    return response.data;
  }

  async createContainer(imageName: string) {
    const response = await this.client.post('/create_container', { image_name: imageName });
    return response.data;
  }

  async deleteContainer(containerId: string) {
    const response = await this.client.delete(`/delete_container/${containerId}`);
    return response.data;
  }

  async reconcileSessions() {
    const response = await this.client.post('/reconcile_sessions');
    return response.data;
  }

  // Cluster APIs
  async getClusters() {
    const response = await this.client.get('/get_clusters');
    return response.data;
  }

  async addCluster(machineName: string, ipAddress?: string, description?: string) {
    const response = await this.client.post('/add_clusters', {
      machine_name: machineName,
      ip_address: ipAddress,
      description,
    });
    return response.data;
  }

  // Template APIs
  async getTemplates() {
    const response = await this.client.get('/get_templates');
    return response.data;
  }

  async addTemplate(imageName: string) {
    const response = await this.client.post('/add_templates', { image_name: imageName });
    return response.data;
  }

  // Admin APIs
  async getAllContainers() {
    const response = await this.client.get('/admin/containers');
    return response.data;
  }

  // Scheduler APIs
  async getSchedulerStatus() {
    const response = await this.client.get('/scheduler/status');
    return response.data;
  }

  async submitSchedulerJob(prompt: string) {
    const response = await this.client.post('/scheduler/submit', { prompt });
    return response.data;
  }

  async tickScheduler() {
    const response = await this.client.post('/scheduler/tick');
    return response.data;
  }

  async resetScheduler() {
    const response = await this.client.post('/scheduler/reset');
    return response.data;
  }

  // Real GPU Metrics APIs (RTX 6000 Worker 연동)
  async getGpuMetrics(gpuId: string) {
    const response = await this.client.get(`/scheduler/metrics/${gpuId}`);
    return response.data;
  }

  async getGpuJobStatus(gpuId: string) {
    const response = await this.client.get(`/scheduler/gpu-status/${gpuId}`);
    return response.data;
  }

  // Health Check
  async healthCheck() {
    const response = await this.client.get('/health');
    return response.data;
  }
}

export const api = new ApiClient();
export default api;
