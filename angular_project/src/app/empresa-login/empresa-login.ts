import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-empresa-login',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './empresa-login.html',
  styleUrl: './empresa-login.css',
})
export class EmpresaLogin {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected logoName = '';

  protected readonly companyForm = this.fb.group({
    companyName: ['Minha Empresa LTDA', [Validators.required, Validators.minLength(2)]],
    corporateEmail: ['contato@empresa.com', [Validators.required, Validators.email]],
    phone: ['(11) 98765-4321', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
    cep: ['01234-567', [Validators.required]],
    state: ['São Paulo', [Validators.required]],
    street: ['Avenida das Nacoes Unidas', [Validators.required]],
    number: ['1500', [Validators.required]],
    city: ['São Paulo', [Validators.required]],
    termsAccepted: [true, [Validators.requiredTrue]],
  });

  protected onLogoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.logoName = input.files?.[0]?.name ?? '';
  }

  protected onSubmit() {
    this.companyForm.markAllAsTouched();
    if (this.companyForm.invalid) {
      return;
    }

    // Fluxo temporario: apos cadastro de empresa, leva para a tela de login.
    this.router.navigateByUrl('/login');
  }
}
