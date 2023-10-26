import Signin from '@/components/Signin';
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Signin',
  description: 'Signin to Hype Note',
};

export default function SigninPage() {
  return (
    <section className="flex pr-[19rem] align-center h-[100vh]">
      <Signin />
    </section>
  );
}
