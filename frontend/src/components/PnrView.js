import { BookingApi } from '../api/client.js';
import { generateTicketHtml } from './PnrTicketCard.js';

export function renderPnrView(container) {
  container.innerHTML = `
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      
      <div class="text-center max-w-xl mx-auto mb-8">
        <div class="inline-flex w-12 h-12 rounded-2xl bg-cyan-500/10 border border-cyan-500/30 items-center justify-center text-cyan-400 mb-3 shadow-lg shadow-cyan-500/10">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
        </div>
        <h2 class="text-2xl sm:text-3xl font-extrabold text-white">Live PNR Status Enquiry</h2>
        <p class="text-xs sm:text-sm text-slate-400 mt-1">
          Public real-time tracking for any 10-digit Indian Railways reservation. Instant chart status & berth allocation.
        </p>
      </div>

      <!-- Search Box -->
      <div class="glass-panel-elevated rounded-2xl p-5 shadow-2xl mb-8 border border-slate-800">
        <form id="pnr-search-form" class="flex flex-col sm:flex-row gap-3">
          <div class="relative flex-1">
            <input 
              type="text" 
              id="pnr-input" 
              placeholder="Enter 10-Digit PNR Number (e.g. 8492019482)"
              maxlength="10"
              required
              class="w-full px-4 py-3.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 font-mono font-bold tracking-widest transition"
            />
          </div>
          <button 
            type="submit" 
            id="pnr-submit-btn" 
            class="px-8 py-3.5 rounded-xl font-bold text-sm bg-gradient-to-r from-cyan-500 to-blue-600 text-slate-950 hover:from-cyan-400 hover:to-blue-500 shadow-lg shadow-cyan-500/25 transition flex items-center justify-center gap-2"
          >
            <span>Track PNR Status</span>
          </button>
        </form>
      </div>

      <!-- PNR Result Container -->
      <div id="pnr-result-box"></div>

    </div>
  `;

  const form = container.querySelector('#pnr-search-form');
  const input = container.querySelector('#pnr-input');
  const resultBox = container.querySelector('#pnr-result-box');

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const pnr = input.value.trim().replace(/\D/g, '');
    if (!pnr || pnr.length !== 10) {
      alert('Please enter a valid 10-digit numeric PNR number');
      return;
    }

    resultBox.innerHTML = `
      <div class="p-8 text-center text-slate-400 glass-panel rounded-2xl">
        <svg class="animate-spin h-6 w-6 text-cyan-400 mx-auto mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
        <span>Fetching live PNR telemetry from Indian Railways core...</span>
      </div>
    `;

    try {
      const b = await BookingApi.getByPnr(pnr);
      resultBox.innerHTML = generateTicketHtml(b);
    } catch (err) {
      resultBox.innerHTML = `
        <div class="p-6 rounded-2xl bg-red-500/10 border border-red-500/30 text-center">
          <p class="text-sm font-bold text-red-400">${err.message || 'PNR not found or invalid'}</p>
        </div>
      `;
    }
  });
}
