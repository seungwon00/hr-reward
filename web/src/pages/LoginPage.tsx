import React from 'react';
import { useNavigate } from 'react-router';
import { login } from '@/api/auth';
import { SignInPage } from '@/components/ui/sign-in';

export default function LoginPage() {
  const navigate = useNavigate();

  const handleSignIn = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    await login({
      email: String(formData.get('email') ?? ''),
      password: String(formData.get('password') ?? ''),
      rememberMe: formData.get('rememberMe') === 'on',
    });
  };

  return (
    <SignInPage
      onSignIn={handleSignIn}
      onResetPassword={() => console.log('비밀번호 찾기 클릭')}
      onCreateAccount={() => navigate('/signup')}
    />
  );
}
