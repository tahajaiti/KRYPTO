import { computed, Injectable, signal } from '@angular/core';

import { UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly userSignal = signal<UserResponse | null>(null);
  private readonly initializedSignal = signal(false);

  readonly user = this.userSignal.asReadonly();
  readonly initialized = this.initializedSignal.asReadonly();

  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly isAnonymous = computed(() => this.initializedSignal() && this.userSignal() === null);

  setAuthenticated(user: UserResponse): void {
    this.userSignal.set(user);
    this.initializedSignal.set(true);
  }

  setAnonymous(): void {
    this.userSignal.set(null);
    this.initializedSignal.set(true);
  }

  setUser(user: UserResponse): void {
    this.userSignal.set(user);
    this.initializedSignal.set(true);
  }
}
