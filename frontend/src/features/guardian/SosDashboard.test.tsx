// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SosDashboard from './SosDashboard';
import { request } from '../../auth/api';
vi.mock('../../auth/api',()=>({request:vi.fn(),ApiError:class extends Error{status=409;}}));
afterEach(()=>{cleanup();vi.clearAllMocks();});
it('restores the persisted countdown and requests server cancellation',async()=>{
 const now=new Date();vi.mocked(request).mockImplementation(async(path,method)=>{
  if(method==='POST')return undefined;
  if(path.startsWith('/guardian/incidents'))return [];
  return [{id:'pending',status:'COUNTDOWN',activationDeadline:new Date(now.getTime()+10000).toISOString(),serverNow:now.toISOString(),incidentId:null}];
 });
 render(<SosDashboard/>);
 await userEvent.setup().click(await screen.findByRole('button',{name:/Cancel SOS/}));
 await waitFor(()=>expect(request).toHaveBeenCalledWith('/guardian/sos/requests/pending/cancel','POST'));
 expect(vi.mocked(request).mock.calls.filter(([path,method])=>path==='/guardian/sos/requests'&&method==='POST')).toHaveLength(0);
});
