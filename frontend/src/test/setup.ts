import '@testing-library/jest-dom/vitest';

import { beforeEach } from 'vitest';

beforeEach(() => { document.cookie = 'XSRF-TOKEN=test-csrf; Path=/'; });
