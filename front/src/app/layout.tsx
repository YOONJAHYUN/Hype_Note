import type { Metadata } from "next";
import Providers from "./providers";
// import { Inter } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";
// const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: {
    default: "한입노트(Hype Note)",
    template: "한입노트(Hype Note)",
  },
  description: "한번에 모든 걸 해결 할 수 있는 나만의 한입 노트!",
  icons: {
    icon: "/favicon.ico",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="dark:bg-dark_background dark:text-font_primary transition-colors duration-1000">
        <Providers>
          <header>
            <Navbar />
          </header>
          <div className="pl-[19rem]">{children}</div>
        </Providers>
      </body>
    </html>
  );
}
