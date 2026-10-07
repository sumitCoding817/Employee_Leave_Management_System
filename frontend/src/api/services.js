import api from './client';
import { cleanParams } from '../utils/format';

const data = (response) => response.data.data; // unwrap ApiResponse { success, message, data }

export const authApi = {
  login: (body) => api.post('/api/auth/login', body).then((r) => r.data), // raw LoginResponse
  register: (body) => api.post('/api/auth/register', body).then(data),
  logout: () => api.post('/api/auth/logout').then((r) => r.data),
};

export const employeeApi = {
  me: () => api.get('/api/employees/me').then(data),
  get: (id) => api.get(`/api/employees/${id}`).then(data),
  list: (params) => api.get('/api/admin/employees', { params: cleanParams(params) }).then(data),
  update: (id, body) => api.put(`/api/employees/${id}`, body).then(data),
  remove: (id) => api.delete(`/api/employees/${id}`).then(data),
  activate: (id) => api.put(`/api/admin/employees/${id}/activate`).then(data),
  deactivate: (id) => api.put(`/api/admin/employees/${id}/deactivate`).then(data),
};

export const departmentApi = {
  list: () => api.get('/api/departments').then(data),
  create: (body) => api.post('/api/departments', body).then(data),
  update: (id, body) => api.put(`/api/departments/${id}`, body).then(data),
  remove: (id) => api.delete(`/api/departments/${id}`).then(data),
};

export const leaveTypeApi = {
  list: () => api.get('/api/leave-types').then(data),
  create: (body) => api.post('/api/leave-types', body).then(data),
  update: (id, body) => api.put(`/api/leave-types/${id}`, body).then(data),
  remove: (id) => api.delete(`/api/leave-types/${id}`).then(data),
};

export const balanceApi = {
  mine: (year) => api.get('/api/leave-balances/my', { params: cleanParams({ year }) }).then(data),
  ofEmployee: (employeeId, year) =>
    api.get(`/api/leave-balances/employee/${employeeId}`, { params: cleanParams({ year }) }).then(data),
  create: (body) => api.post('/api/leave-balances', body).then(data),
  update: (id, body) => api.put(`/api/leave-balances/${id}`, body).then(data),
};

export const leaveApi = {
  apply: (body) => api.post('/api/leaves', body).then(data),
  mine: (params) => api.get('/api/leaves/my', { params: cleanParams(params) }).then(data),
  myLeave: (id) => api.get(`/api/leaves/my/${id}`).then(data),
  cancel: (id) => api.put(`/api/leaves/${id}/cancel`).then(data),
  approve: (id) => api.put(`/api/leaves/${id}/approve`).then(data),
  reject: (id, rejectionReason) => api.put(`/api/leaves/${id}/reject`, { rejectionReason }).then(data),
  all: (params) => api.get('/api/admin/leaves', { params: cleanParams(params) }).then(data),
};

export const dashboardApi = {
  employee: () => api.get('/api/dashboard/employee').then(data),
  admin: () => api.get('/api/dashboard/admin').then(data),
};
