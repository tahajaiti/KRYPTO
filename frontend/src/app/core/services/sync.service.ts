import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SyncService {
  private readonly refreshCounter = signal(0);
  
  readonly syncTrigger = this.refreshCounter.asReadonly();

  triggerGlobalRefresh(): void {
    this.refreshCounter.update(count => count + 1);
  }
}
