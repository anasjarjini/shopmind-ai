export interface User {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  city?: string;
  address?: string;
  role: 'USER' | 'ADMIN';
}

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}
