// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CareCircle from './CareCircle';
import { request } from '../../auth/api';
vi.mock('../../auth/api',()=>({request:vi.fn()}));
afterEach(()=>{cleanup();vi.clearAllMocks();});
it('requires invitee acceptance and exposes pending incoming actions',async()=>{
 vi.mocked(request).mockResolvedValue([{id:'m',careRecipientId:'owner',inviteeId:'me',recipientName:'Family member',inviteeName:'Me',relationshipType:'GUARDIAN',status:'PENDING'}]);
 render(<CareCircle userId="me"/>); const user=userEvent.setup();
 await user.click(screen.getByRole('button',{name:/Incoming/}));
 await user.click(await screen.findByRole('button',{name:/^Accept/}));
 await waitFor(()=>expect(request).toHaveBeenCalledWith('/guardian/care-circle/memberships/m/accept','POST'));
});
