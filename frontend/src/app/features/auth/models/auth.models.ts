export type UserRole = 'PLAYER' | 'ADMIN';

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  avatar: string | null;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  tutorialCompleted: boolean;
}

export interface UserLookupResponse {
  id: string;
  username: string;
  email: string;
  avatar: string | null;
}

export interface AuthResponse {
  user: UserResponse;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface UpdateProfileRequest {
  username?: string;
  avatar?: string;
}
