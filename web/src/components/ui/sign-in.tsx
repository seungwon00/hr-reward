import React from 'react';
import { AuthHero, Field, PasswordField } from '@/components/ui/auth-shared';

interface SignInPageProps {
  title?: React.ReactNode;
  description?: React.ReactNode;
  onSignIn?: (event: React.FormEvent<HTMLFormElement>) => void;
  onResetPassword?: () => void;
  onCreateAccount?: () => void;
}

export const SignInPage: React.FC<SignInPageProps> = ({
  title = <span className="font-light text-foreground tracking-tighter">로그인</span>,
  description = '계정에 로그인하고 오늘의 근무를 시작하세요',
  onSignIn,
  onResetPassword,
  onCreateAccount,
}) => (
  <div className="min-h-[100dvh] md:h-[100dvh] flex flex-col md:flex-row font-geist w-full">
    {/* 왼쪽: 로그인 폼 */}
    <section className="flex-1 flex items-center justify-center p-8">
      <div className="w-full max-w-md">
        <div className="flex flex-col gap-6">
          <h1 className="animate-element animate-delay-100 text-4xl md:text-5xl font-semibold leading-tight">{title}</h1>
          <p className="animate-element animate-delay-200 text-muted-foreground">{description}</p>

          <form className="space-y-5" onSubmit={onSignIn}>
            <Field
              className="animate-element animate-delay-300"
              id="email" name="email" type="email" label="이메일"
              placeholder="이메일 주소를 입력하세요" autoComplete="email" required
            />

            <PasswordField
              className="animate-element animate-delay-400"
              id="password" name="password" label="비밀번호"
              placeholder="비밀번호를 입력하세요" autoComplete="current-password" required
            />

            <div className="animate-element animate-delay-500 flex items-center justify-between text-sm">
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" name="rememberMe" className="custom-checkbox" />
                <span className="text-foreground/90">로그인 유지</span>
              </label>
              <a href="#" onClick={(e) => { e.preventDefault(); onResetPassword?.(); }} className="hover:underline text-violet-500 transition-colors">비밀번호 찾기</a>
            </div>

            <button type="submit" className="animate-element animate-delay-600 w-full rounded-2xl bg-primary py-4 font-medium text-primary-foreground hover:bg-primary/90 transition-colors">
              로그인
            </button>
          </form>

          {/* TODO: 카카오 로그인 버튼 자리 (구분선 "또는" + 카카오 로그인 버튼, animate-delay-700/800) */}

          <p className="animate-element animate-delay-700 text-center text-sm text-muted-foreground">
            아직 계정이 없으신가요? <a href="#" onClick={(e) => { e.preventDefault(); onCreateAccount?.(); }} className="text-violet-500 hover:underline transition-colors">회원가입</a>
          </p>
        </div>
      </div>
    </section>

    {/* 오른쪽: 서비스 소개 */}
    <AuthHero />
  </div>
);
