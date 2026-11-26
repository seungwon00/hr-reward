import React from 'react';
import { AuthHero, Field, PasswordField } from '@/components/ui/auth-shared';

interface SignUpPageProps {
  title?: React.ReactNode;
  description?: React.ReactNode;
  errorMessage?: string | null;
  onSignUp?: (event: React.FormEvent<HTMLFormElement>) => void;
  onSignIn?: () => void;
}

export const SignUpPage: React.FC<SignUpPageProps> = ({
  title = <span className="font-light text-foreground tracking-tighter">회원가입</span>,
  description = '계정을 만들고 매장 관리를 시작하세요',
  errorMessage,
  onSignUp,
  onSignIn,
}) => (
  <div className="min-h-[100dvh] md:h-[100dvh] flex flex-col md:flex-row font-geist w-full">
    {/* 왼쪽: 회원가입 폼 */}
    <section className="flex-1 flex items-center justify-center p-8 md:overflow-y-auto">
      <div className="w-full max-w-md">
        <div className="flex flex-col gap-6">
          <h1 className="animate-element animate-delay-100 text-4xl md:text-5xl font-semibold leading-tight">{title}</h1>
          <p className="animate-element animate-delay-200 text-muted-foreground">{description}</p>

          <form className="space-y-4" onSubmit={onSignUp}>
            <Field
              className="animate-element animate-delay-300"
              id="name" name="name" type="text" label="이름"
              placeholder="이름을 입력하세요" autoComplete="name" required
            />

            <Field
              className="animate-element animate-delay-300"
              id="email" name="email" type="email" label="이메일"
              placeholder="이메일 주소를 입력하세요" autoComplete="email" required
            />

            <Field
              className="animate-element animate-delay-400"
              id="phone" name="phone" type="tel" label="휴대폰 번호"
              placeholder="010-1234-5678" autoComplete="tel"              pattern="01[0-9]-?[0-9]{3,4}-?[0-9]{4}" title="예: 010-1234-5678" required
            />

            <PasswordField
              className="animate-element animate-delay-400"
              id="password" name="password" label="비밀번호"
              placeholder="8자 이상 입력하세요" autoComplete="new-password" minLength={8} required
            />

            <PasswordField
              className="animate-element animate-delay-500"
              id="passwordConfirm" name="passwordConfirm" label="비밀번호 확인"
              placeholder="비밀번호를 한 번 더 입력하세요" autoComplete="new-password" minLength={8} required
            />

            {errorMessage && (
              <p role="alert" className="text-sm text-destructive">{errorMessage}</p>
            )}

            <button type="submit" className="animate-element animate-delay-600 w-full rounded-2xl bg-primary py-4 font-medium text-primary-foreground hover:bg-primary/90 transition-colors">
              회원가입
            </button>
          </form>

          {/* TODO: 카카오로 시작하기 버튼 자리 */}

          <p className="animate-element animate-delay-700 text-center text-sm text-muted-foreground">
            이미 계정이 있으신가요? <a href="#" onClick={(e) => { e.preventDefault(); onSignIn?.(); }} className="text-violet-500 hover:underline transition-colors">로그인</a>
          </p>
        </div>
      </div>
    </section>

    {/* 오른쪽: 서비스 소개 */}
    <AuthHero />
  </div>
);
