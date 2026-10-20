import { store } from '../state/store.js';

export function formatPnr(pnr) {
  if (!pnr) return '';
  const clean = pnr.replace(/\D/g, '');
  if (clean.length === 10) {
    return `${clean.slice(0, 3)}-${clean.slice(3, 6)}-${clean.slice(6)}`;
  }
  return pnr;
}

export function generateTicketHtml(b, isModal = false) {
  const isConfirmed = b.bookingStatus === 'CONFIRMED' || b.bookingStatus === 'PARTIALLY_CONFIRMED';
  const isCancelled = b.bookingStatus === 'CANCELLED';
  const isRac = b.bookingStatus === 'RAC';

  let statusBadgeClass = 'bg-amber-500/20 text-amber-300 border-amber-500/40';
  if (isConfirmed) statusBadgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
  else if (isCancelled) statusBadgeClass = 'bg-red-500/20 text-red-300 border-red-500/40';
  else if (isRac) statusBadgeClass = 'bg-blue-500/20 text-blue-300 border-blue-500/40';

  const formattedPnr = formatPnr(b.pnrNumber);

  return `
    <div class="e-ticket-card rounded-2xl glass-panel-elevated border border-slate-700/80 p-6 shadow-2xl relative overflow-hidden print:border-black print:text-black print:bg-white">
      
      <!-- Top Decorative Railway Watermark Strip -->
      <div class="flex items-center justify-between pb-5 border-b border-slate-800 print:border-slate-300">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-slate-950 font-bold shadow-lg shadow-cyan-500/20 print:bg-slate-200">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 stroke-slate-950 fill-none stroke-[2.2]" viewBox="0 0 24 24">
              <rect width="16" height="16" x="4" y="3" rx="2"/>
              <path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/><circle cx="8" cy="15" r="1"/><circle cx="16" cy="15" r="1"/>
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-extrabold text-xl text-white font-mono tracking-tight print:text-black">
                Rail<span class="text-cyan-400 print:text-black">Connect</span>
              </span>
              <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-cyan-950 text-cyan-300 border border-cyan-800 uppercase print:border-black print:text-black">
                IRCTC Electronic Reservation Slip (ERS)
              </span>
            </div>
            <p class="text-xs text-slate-400 print:text-slate-600">Issued via RailConnect High-Throughput Engine</p>
          </div>
        </div>

        <!-- PNR Pill -->
        <div class="text-right">
          <span class="text-[10px] text-slate-400 uppercase tracking-widest block font-medium">10-Digit PNR</span>
          <div class="inline-flex items-center gap-2 mt-0.5">
            <span class="px-3.5 py-1.5 rounded-xl bg-slate-950 border border-cyan-500/40 text-cyan-400 font-mono font-extrabold text-lg tracking-widest shadow-inner print:border-black print:text-black">
              ${formattedPnr}
            </span>
          </div>
        </div>
      </div>

      <!-- Ticket Journey Details Strip -->
      <div class="my-5 p-4 rounded-xl bg-slate-950/80 border border-slate-800/80 grid grid-cols-1 md:grid-cols-4 gap-4 print:bg-slate-50 print:border-slate-300">
        <div>
          <span class="text-[10px] text-slate-500 uppercase tracking-wider block font-semibold">Train Number & Name</span>
          <p class="text-sm font-bold text-white mt-0.5 print:text-black">#${b.trainNumber} - ${b.trainName}</p>
          <p class="text-xs text-cyan-400 font-mono mt-0.5 print:text-slate-600">Class: <strong>${b.classCode}</strong> • Quota: <strong>${b.quotaType}</strong></p>
        </div>

        <div>
          <span class="text-[10px] text-slate-500 uppercase tracking-wider block font-semibold">Boarding Station</span>
          <p class="text-base font-extrabold text-white mt-0.5 font-mono print:text-black">${b.fromStationCode}</p>
          <p class="text-xs text-slate-400 print:text-slate-600">Dep: <strong class="text-white print:text-black">${b.departureTime || '06:00'}</strong></p>
        </div>

        <div>
          <span class="text-[10px] text-slate-500 uppercase tracking-wider block font-semibold">Destination Station</span>
          <p class="text-base font-extrabold text-white mt-0.5 font-mono print:text-black">${b.toStationCode}</p>
          <p class="text-xs text-slate-400 print:text-slate-600">Arr: <strong class="text-white print:text-black">${b.arrivalTime || '18:15'}</strong></p>
        </div>

        <div class="text-left md:text-right">
          <span class="text-[10px] text-slate-500 uppercase tracking-wider block font-semibold">Journey Date & Status</span>
          <p class="text-sm font-bold text-cyan-300 font-mono mt-0.5 print:text-black">${b.journeyDate}</p>
          <span class="inline-block mt-1 px-2.5 py-0.5 rounded-full text-xs font-extrabold ${statusBadgeClass} border uppercase print:border-black print:text-black">
            ${b.bookingStatus}
          </span>
        </div>
      </div>

      <!-- Passengers Table -->
      <div class="my-5">
        <h4 class="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-3 print:text-black">
          Passenger Details & Current Status:
        </h4>
        
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs border border-slate-800 rounded-xl overflow-hidden print:border-slate-300">
            <thead class="bg-slate-950 text-slate-400 font-mono uppercase text-[10px] border-b border-slate-800 print:bg-slate-100 print:text-black">
              <tr>
                <th class="py-2.5 px-3">#</th>
                <th class="py-2.5 px-3">Passenger Name</th>
                <th class="py-2.5 px-3">Age / Gender</th>
                <th class="py-2.5 px-3">Berth Pref</th>
                <th class="py-2.5 px-3">Booking Status</th>
                <th class="py-2.5 px-3 text-right">Current Live Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/80 font-sans print:divide-slate-200">
              ${(b.passengers || []).map((p, idx) => {
                const isCnf = p.currentStatus === 'CNF';
                const isRac = p.currentStatus === 'RAC';
                const isWl = p.currentStatus === 'WL';

                let liveBadgeClass = 'bg-slate-900 text-slate-300 border-slate-800';
                if (isCnf) liveBadgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
                else if (isRac) liveBadgeClass = 'bg-blue-500/20 text-blue-300 border-blue-500/40';
                else if (isWl) liveBadgeClass = 'bg-amber-500/20 text-amber-300 border-amber-500/40';

                return `
                  <tr class="hover:bg-slate-900/40 print:hover:bg-white">
                    <td class="py-3 px-3 font-mono text-slate-500">${idx + 1}</td>
                    <td class="py-3 px-3 font-bold text-white print:text-black">${p.passengerName}</td>
                    <td class="py-3 px-3 text-slate-300 print:text-black">${p.age} yrs / ${p.gender}</td>
                    <td class="py-3 px-3 text-slate-400 font-mono">${p.berthPreference || 'NO_PREF'}</td>
                    <td class="py-3 px-3 font-mono font-semibold text-slate-300 print:text-black">
                      ${p.bookingStatusDetails || p.bookingStatus}
                    </td>
                    <td class="py-3 px-3 text-right">
                      <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono font-extrabold ${liveBadgeClass} border print:border-black print:text-black">
                        ${p.currentCoach ? `${p.currentCoach} - ${p.currentSeatNumber} (${p.currentBerthType})` : p.currentStatusDetails || p.currentStatus}
                      </span>
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      </div>

      <!-- Payment & Transaction Info Footer -->
      <div class="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 print:border-slate-300">
        <div class="text-xs text-slate-400 print:text-slate-600">
          <span>Txn Ref: <strong class="font-mono text-white print:text-black">${b.transactionReference || 'TXN-' + b.pnrNumber}</strong></span>
          <span class="mx-2">•</span>
          <span>Payment: <strong class="text-emerald-400 print:text-black">${b.paymentStatus || 'SUCCESS'}</strong></span>
          ${b.refundAmount ? `<span class="mx-2">•</span><span class="text-rose-400 font-bold">Refund: ₹${b.refundAmount}</span>` : ''}
        </div>

        <div class="flex items-center gap-3">
          <div class="text-right">
            <span class="text-[10px] text-slate-500 uppercase block font-semibold">Total Fare</span>
            <span class="text-xl font-extrabold text-white font-mono print:text-black">₹${b.totalFare}</span>
          </div>

          <button 
            type="button"
            data-download-pdf="${b.pnrNumber}"
            class="px-4 py-2 rounded-xl text-xs font-bold bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 transition flex items-center gap-1.5 print:hidden shadow-md shadow-cyan-500/20 active:scale-95"
            title="Download Official IRCTC ERS PDF with embedded QR Code"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span>Download PDF</span>
          </button>

          <button 
            onclick="window.print()" 
            class="px-4 py-2 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-slate-200 border border-slate-700 transition flex items-center gap-1.5 print:hidden shadow-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect width="12" height="8" x="6" y="14"/>
            </svg>
            <span>Print E-Ticket</span>
          </button>
        </div>
      </div>

    </div>
  `;
}

export function renderPnrTicketModal(container) {
  const state = store.getState();
  const { activeModal, activeTicket } = state;

  if (activeModal !== 'ticket' || !activeTicket) {
    return;
  }

  const modalWrapper = document.createElement('div');
  modalWrapper.id = 'pnr-ticket-modal-backdrop';
  modalWrapper.className = 'fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-950/80 backdrop-blur-md overflow-y-auto animate-fade-in';

  modalWrapper.innerHTML = `
    <div class="relative w-full max-w-3xl my-6">
      
      <!-- Close floating button -->
      <div class="flex justify-end mb-2 print:hidden">
        <button id="close-ticket-modal-btn" class="p-2 rounded-xl text-slate-400 hover:text-white bg-slate-900/90 border border-slate-800 hover:border-slate-700 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      ${generateTicketHtml(activeTicket, true)}

      <div class="mt-4 flex justify-center gap-3 print:hidden">
        <button id="ticket-done-btn" class="px-6 py-2.5 rounded-xl font-bold text-xs bg-cyan-500 hover:bg-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/25 transition">
          Done / Back to Home
        </button>
      </div>
    </div>
  `;

  container.appendChild(modalWrapper);

  modalWrapper.querySelector('#close-ticket-modal-btn')?.addEventListener('click', () => {
    store.closeModal();
  });

  modalWrapper.querySelector('#ticket-done-btn')?.addEventListener('click', () => {
    store.closeModal();
    store.setActiveTab('bookings');
  });
}
