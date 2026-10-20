import { store } from '../state/store.js';
import { BookingApi, NotificationApi } from '../api/client.js';
import { formatPnr } from './PnrTicketCard.js';

export function renderMyBookingsView(container) {
  const { currentUser } = store.getState();

  if (!currentUser) {
    container.innerHTML = `
      <div class="max-w-md mx-auto px-4 py-16 text-center">
        <h3 class="text-xl font-bold text-white mb-2">Sign in to view your bookings</h3>
        <p class="text-xs text-slate-400 mb-6">Manage all your IRCTC confirmed tickets, cancellations, and refunds.</p>
        <button id="bookings-signin-btn" class="px-6 py-2.5 rounded-xl font-bold text-xs bg-cyan-500 text-slate-950 hover:bg-cyan-400 transition">
          Sign In Now
        </button>
      </div>
    `;
    container.querySelector('#bookings-signin-btn')?.addEventListener('click', () => {
      store.setAuthTab('login');
      store.openModal('auth');
    });
    return;
  }

  container.innerHTML = `
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 pb-4 border-b border-slate-800">
        <div>
          <h2 class="text-2xl font-extrabold text-white">My Railway Bookings</h2>
          <p class="text-xs text-slate-400 mt-1">Passenger tickets booked under account: <strong>${currentUser.email}</strong></p>
        </div>
        <button id="refresh-bookings-btn" class="px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-xs font-semibold text-slate-300 hover:text-white transition flex items-center gap-1.5 self-start">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 21h5v-5"/></svg>
          Refresh
        </button>
      </div>

      <div id="bookings-list-container">
        <div class="text-center py-12 text-slate-500">
          <svg class="animate-spin h-6 w-6 text-cyan-400 mx-auto mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
          Loading your travel history...
        </div>
      </div>

      <!-- Cancellation Confirmation Modal Container -->
      <div id="cancellation-modal-container"></div>

    </div>
  `;

  const listContainer = container.querySelector('#bookings-list-container');
  const refreshBtn = container.querySelector('#refresh-bookings-btn');
  const cancelModalContainer = container.querySelector('#cancellation-modal-container');

  async function loadBookings() {
    try {
      const bookings = await BookingApi.getMyBookings();
      if (!bookings || bookings.length === 0) {
        listContainer.innerHTML = `
          <div class="glass-panel rounded-2xl p-12 text-center border border-slate-800">
            <div class="w-12 h-12 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-500 mx-auto mb-3">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <p class="text-slate-300 font-semibold text-sm">No bookings found for this account yet.</p>
            <p class="text-xs text-slate-500 mt-1 mb-5">Search train routes and book your first journey now.</p>
            <button id="book-first-ticket-btn" class="px-5 py-2.5 rounded-xl text-xs font-bold bg-cyan-500 text-slate-950 hover:bg-cyan-400 shadow-md shadow-cyan-500/20 transition">
              Search Trains & Book Ticket
            </button>
          </div>
        `;
        listContainer.querySelector('#book-first-ticket-btn')?.addEventListener('click', () => {
          store.setActiveTab('search');
        });
        return;
      }

      listContainer.innerHTML = `
        <div class="space-y-4">
          ${bookings.map(b => {
            const isConfirmed = b.bookingStatus === 'CONFIRMED' || b.bookingStatus === 'PARTIALLY_CONFIRMED';
            const isCancelled = b.bookingStatus === 'CANCELLED';
            const isRac = b.bookingStatus === 'RAC';

            let badgeClass = 'bg-amber-500/20 text-amber-300 border-amber-500/40';
            if (isConfirmed) badgeClass = 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
            else if (isCancelled) badgeClass = 'bg-red-500/20 text-red-300 border-red-500/40';
            else if (isRac) badgeClass = 'bg-blue-500/20 text-blue-300 border-blue-500/40';

            return `
              <div class="glass-panel rounded-2xl p-5 border border-slate-800 space-y-4 hover:border-slate-700 transition">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/80 pb-3">
                  <div>
                    <div class="flex items-center gap-2.5">
                      <span class="px-2.5 py-0.5 rounded-lg bg-cyan-950 font-mono text-xs font-bold text-cyan-400 border border-cyan-800/50">
                        PNR: ${formatPnr(b.pnrNumber)}
                      </span>
                      <h3 class="text-sm font-bold text-white">#${b.trainNumber} - ${b.trainName}</h3>
                      <span class="px-2 py-0.2 rounded-full text-[10px] font-extrabold ${badgeClass} border uppercase">
                        ${b.bookingStatus}
                      </span>
                    </div>
                    <p class="text-[11px] text-slate-400 mt-1">
                      Journey: <strong>${b.journeyDate}</strong> • ${b.fromStationCode} (${b.departureTime || '06:00'}) → ${b.toStationCode} (${b.arrivalTime || '18:15'}) • Class ${b.classCode} (${b.quotaType})
                    </p>
                  </div>

                  <div class="flex items-center gap-3">
                    <div class="text-right">
                      <span class="text-[10px] text-slate-500 uppercase block">Total Fare</span>
                      <span class="font-mono text-sm font-bold text-white">₹${b.totalFare}</span>
                      ${b.refundAmount ? `<span class="text-[10px] text-rose-400 block font-mono font-bold">Refund: ₹${b.refundAmount}</span>` : ''}
                    </div>

                    <button 
                      data-view-pnr="${b.pnrNumber}" 
                      class="view-eticket-btn px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-xs font-semibold text-cyan-400 hover:text-cyan-300 transition"
                    >
                      E-Ticket
                    </button>

                    <button 
                      type="button"
                      data-download-pdf="${b.pnrNumber}" 
                      class="download-pdf-btn px-3 py-1.5 rounded-xl bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-xs font-semibold text-cyan-300 transition flex items-center gap-1"
                      title="Download PDF E-Ticket"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                      </svg>
                      <span>PDF</span>
                    </button>

                    ${!isCancelled ? `
                      <button 
                        data-cancel-pnr="${b.pnrNumber}" 
                        data-total-fare="${b.totalFare}"
                        data-train-name="${b.trainName}"
                        class="cancel-booking-btn px-3 py-1.5 rounded-xl text-xs font-semibold text-red-400 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 transition"
                      >
                        Cancel Ticket
                      </button>
                    ` : ''}
                  </div>
                </div>

                <!-- Passengers mini table -->
                <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                  ${(b.passengers || []).map(p => {
                    const isCnf = p.currentStatus === 'CNF';
                    return `
                      <div class="p-2.5 rounded-xl bg-slate-950 border border-slate-800/60 flex items-center justify-between text-xs">
                        <div>
                          <p class="font-bold text-slate-200">${p.passengerName}</p>
                          <p class="text-[10px] text-slate-500">${p.age}y • ${p.gender}</p>
                        </div>
                        <span class="font-mono text-[11px] font-bold ${isCnf ? 'text-emerald-400' : 'text-amber-400'}">
                          ${p.currentCoach ? `${p.currentCoach}-${p.currentSeatNumber}` : p.currentStatus}
                        </span>
                      </div>
                    `;
                  }).join('')}
                </div>
              </div>
            `;
          }).join('')}
        </div>
      `;

      // View E-ticket handlers
      listContainer.querySelectorAll('.view-eticket-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const pnr = btn.getAttribute('data-view-pnr');
          const booking = bookings.find(x => x.pnrNumber === pnr);
          if (booking) {
            store.openTicketModal(booking);
          }
        });
      });

      // Cancel button listeners with refund modal
      listContainer.querySelectorAll('.cancel-booking-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const pnr = btn.getAttribute('data-cancel-pnr');
          const totalFare = parseFloat(btn.getAttribute('data-total-fare') || 0);
          const trainName = btn.getAttribute('data-train-name');
          const refundAmount = (totalFare * 0.75).toFixed(2);

          showCancelModal(pnr, trainName, totalFare, refundAmount);
        });
      });

    } catch (err) {
      listContainer.innerHTML = `
        <div class="p-6 rounded-2xl bg-red-500/10 border border-red-500/30 text-center">
          <p class="text-xs text-red-400 font-bold">${err.message || 'Failed to load bookings'}</p>
        </div>
      `;
    }
  }

  function showCancelModal(pnr, trainName, totalFare, refundAmount) {
    cancelModalContainer.innerHTML = `
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fade-in">
        <div class="w-full max-w-md rounded-2xl glass-panel-elevated border border-slate-700/80 p-6 shadow-2xl space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400 font-bold">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <div>
              <h3 class="text-base font-bold text-white">Confirm Ticket Cancellation</h3>
              <p class="text-xs text-slate-400">PNR: <strong class="font-mono text-cyan-400">${formatPnr(pnr)}</strong> • ${trainName}</p>
            </div>
          </div>

          <div class="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
            <div class="flex items-center justify-between">
              <span class="text-slate-400">Original Total Fare:</span>
              <span class="font-mono text-white font-bold">₹${totalFare}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-400">IRCTC Cancellation Charge (25%):</span>
              <span class="font-mono text-red-400 font-bold">-₹${(totalFare * 0.25).toFixed(2)}</span>
            </div>
            <div class="pt-2 border-t border-slate-900 flex items-center justify-between text-sm">
              <span class="font-bold text-white">Refund to Original Payment (75%):</span>
              <span class="font-mono text-emerald-400 font-extrabold">₹${refundAmount}</span>
            </div>
          </div>

          <p class="text-[11px] text-slate-400">
            Cancelling this ticket will immediately release allocated berths back to waitlisted and RAC passengers via Kafka event promotion.
          </p>

          <div class="flex items-center justify-end gap-3 pt-2">
            <button id="cancel-dismiss-btn" class="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
              Keep Ticket
            </button>
            <button id="cancel-confirm-btn" class="px-5 py-2 rounded-xl text-xs font-bold bg-red-500 hover:bg-red-400 text-slate-950 transition">
              Confirm Cancellation
            </button>
          </div>
        </div>
      </div>
    `;

    cancelModalContainer.querySelector('#cancel-dismiss-btn')?.addEventListener('click', () => {
      cancelModalContainer.innerHTML = '';
    });

    cancelModalContainer.querySelector('#cancel-confirm-btn')?.addEventListener('click', async () => {
      const btn = cancelModalContainer.querySelector('#cancel-confirm-btn');
      btn.disabled = true;
      btn.textContent = 'Cancelling...';

      try {
        const cancelled = await BookingApi.cancelBooking(pnr);
        cancelModalContainer.innerHTML = '';
        alert(`Booking ${pnr} cancelled successfully. Refund of ₹${cancelled.refundAmount || refundAmount} processed.`);

        // Refresh user notifications to capture potential promotions
        NotificationApi.getMyNotifications()
          .then(notifs => store.setNotifications(notifs || []))
          .catch(() => {});

        loadBookings();
      } catch (err) {
        btn.disabled = false;
        btn.textContent = 'Confirm Cancellation';
        alert(`Cancellation failed: ${err.message}`);
      }
    });
  }

  refreshBtn?.addEventListener('click', loadBookings);
  loadBookings();
}
