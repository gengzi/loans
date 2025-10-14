/** @type {import('next').NextConfig} */
module.exports = {
  output: "standalone",
  experimental: {
    // This is needed for standalone output to work correctly
    outputFileTracingRoot: undefined,
  },
  // 添加API请求代理配置
  async rewrites() {
    return [
      {
        source: '/chat/:path*',
        destination: 'http://localhost:8883/chat/:path*',
      },
    ];
  },
};
