import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Pesira Employee Records",
  description: "Search, manage and export employee records",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
