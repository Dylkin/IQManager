import { Component } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AuthService } from './services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CommonModule],
  template: `
    <div class="d-flex min-vh-100">
      <app-navbar *ngIf="showNavbar"></app-navbar>
      <main class="flex-fill p-4" [style.margin-left]="showNavbar ? '260px' : '0'">
        <div class="container-fluid" style="max-width: 1400px;">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
})
export class AppComponent {
  title = 'IQManager';

  constructor(private router: Router, private authService: AuthService) {}

  get showNavbar(): boolean {
    return this.router.url !== '/login' && !this.router.url.startsWith('/login');
  }
}
