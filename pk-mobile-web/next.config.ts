import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  typescript: {
    // Mengabaikan error TypeScript saat proses build di Netlify / Vercel
    ignoreBuildErrors: true,
  },
};

export default nextConfig;