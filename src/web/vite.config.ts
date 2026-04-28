import { defineConfig } from 'vitest/config';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Body files live in the Java resources tree.  vite-plugin-static-copy
// serves them under /shapes/ in dev mode and copies them into dist/shapes/
// during production builds — no duplication in version control.
export default defineConfig({
  base: './',
  plugins: [
    viteStaticCopy({
      targets: [
        {
          src: path.resolve(
            __dirname,
            '../main/resources/com/ullgren/modern/simple3d/*.{body,list}',
          ),
          dest: 'shapes',
        },
      ],
    }),
  ],
  test: {
    environment: 'node',
  },
});
