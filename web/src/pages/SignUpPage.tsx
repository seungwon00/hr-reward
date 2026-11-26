import React, { useState } from 'react';
import { useNavigate } from 'react-router';
import { signUp } from '@/api/auth';
import { SignUpPage as SignUpForm } from '@/components/ui/sign-up';

export default function SignUpPage() {
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSignUp = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const password = String(formData.get('password') ?? '');

    if (password !== formData.get('passwordConfirm')) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }
    setErrorMessage(null);

    await signUp({
      name: String(formData.get('name') ?? ''),
      email: String(formData.get('email') ?? ''),
      phone: String(formData.get('phone') ?? '').replace(/-/g, ''),
      password,
    });
  };

  return (
    <SignUpForm
      errorMessage={errorMessage}
      onSignUp={handleSignUp}
      onSignIn={() => navigate('/login')}
    />
  );
}
