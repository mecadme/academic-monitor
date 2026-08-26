import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

const healthResponse = {
  status: 'UP',
  checkedAt: new Date().toISOString(),
  services: [
    { name: 'backend', status: 'UP', message: 'Spring Boot API is running' },
    { name: 'database', status: 'UP', message: 'PostgreSQL is reachable' },
    { name: 'ai', status: 'UP', message: 'Ollama is reachable' },
  ],
};

describe('App', () => {
  it('renders service health from the backend', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, json: async () => healthResponse }),
    );

    render(<App />);

    expect(screen.getByRole('heading', { name: /estado del sistema/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Todos los servicios estan online')).toBeInTheDocument();
    });

    expect(screen.getByText('Frontend')).toBeInTheDocument();
    expect(screen.getByText('Backend')).toBeInTheDocument();
    expect(screen.getByText('Database')).toBeInTheDocument();
    expect(screen.getByText('AI Service')).toBeInTheDocument();
  });
});

