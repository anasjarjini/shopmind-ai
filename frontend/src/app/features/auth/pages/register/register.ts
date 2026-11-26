import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';

function passwordStrength(control: AbstractControl): ValidationErrors | null {
  const v = control.value as string;
  if (!v) return null;
  const ok = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+\-=\[\]{};':"\\|,.<>\/]).{8,}$/.test(v);
  return ok ? null : { passwordStrength: true };
}

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const pw  = group.get('password')?.value;
  const cpw = group.get('confirmPassword')?.value;
  return pw === cpw ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register {
  private readonly fb           = inject(FormBuilder);
  private readonly authService  = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly router       = inject(Router);

  readonly loading      = signal(false);
  readonly showPassword = signal(false);

  readonly form = this.fb.group(
    {
      fullName:        ['', [Validators.required, Validators.minLength(2)]],
      email:           ['', [Validators.required, Validators.email]],
      password:        ['', [Validators.required, Validators.minLength(8), passwordStrength]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatch }
  );

  get fullName()        { return this.form.get('fullName')!; }
  get email()           { return this.form.get('email')!; }
  get password()        { return this.form.get('password')!; }
  get confirmPassword() { return this.form.get('confirmPassword')!; }

  /** Password strength score 0-4 */
  get strengthScore(): number {
    const v = this.password.value ?? '';
    let s = 0;
    if (v.length >= 8)             s++;
    if (/[a-z]/.test(v))           s++;
    if (/[A-Z]/.test(v))           s++;
    if (/\d/.test(v))              s++;
    if (/[@$!%*?&#^()_+\-=]/.test(v)) s++;
    return s;
  }

  get strengthLabel(): string {
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong', 'Very Strong'];
    return labels[this.strengthScore] ?? '';
  }

  get strengthColor(): string {
    const colors = ['', 'bg-red-500', 'bg-orange-400', 'bg-yellow-400', 'bg-green-400', 'bg-green-600'];
    return colors[this.strengthScore] ?? '';
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);

    const { fullName, email, password } = this.form.value as {
      fullName: string; email: string; password: string;
    };

    this.authService.register({ fullName, email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.notification.success('Account created! Welcome to EShop 🎉');
        this.router.navigate(['/auth/login']);
      },
      error:    () => this.loading.set(false),
      complete: () => this.loading.set(false)
    });
  }
}
