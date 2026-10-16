import { store } from '../state/store.js';
import { InventoryApi } from '../api/client.js';

let countdownInterval = null;
let remainingSeconds = 300;

export function renderCoachPickerModal(container) {
  const state = store.getState();
  const { activeModal, activeCoachPicker, lockedSeats, selectedQuota } = state;

  if (activeModal !== 'coachPicker' || !activeCoachPicker) {
    return;
  }

  const {
    trainNumber,
    trainName,
    fromStationCode,
    toStationCode,
    journeyDate,
    classCode,
    fare,
    departureTime,
    arrivalTime,
  } = activeCoachPicker;

  // Modal scaffold
  const modalWrapper = document.createElement('div');
  modalWrapper.id = 'coach-picker-modal-backdrop';
  modalWrapper.className = 'fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-950/80 backdrop-blur-md overflow-y-auto animate-fade-in';

  modalWrapper.innerHTML = `
    <div class="relative w-full max-w-4xl rounded-2xl glass-panel-elevated border border-slate-700/80 p-5 sm:p-6 shadow-2xl my-6 flex flex-col max-h-[92vh]">
      
      <!-- Top Header -->
      <div class="flex items-center justify-between pb-4 border-b border-slate-800">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="16" height="16" x="4" y="3" rx="2"/><path d="M4 11h16"/><path d="M12 3v8"/><path d="m8 19-2 3"/><path d="m18 22-2-3"/><circle cx="8" cy="15" r="1"/><circle cx="16" cy="15" r="1"/>
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-extrabold text-white text-base sm:text-lg">Coach & Berth Selection</span>
              <span class="px-2 py-0.5 rounded font-mono font-bold text-xs bg-cyan-500/20 text-cyan-300 border border-cyan-500/40">
                ${classCode} (${selectedQuota})
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">
              #${trainNumber} ${trainName} • ${fromStationCode} → ${toStationCode} • ${journeyDate}
            </p>
          </div>
        </div>

        <button id="close-coach-picker-btn" class="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-850 border border-transparent hover:border-slate-700 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- Live Redis Lock Countdown Timer Banner -->
      <div id="redis-lock-banner" class="${lockedSeats.length > 0 ? '' : 'hidden'} mt-3 p-3 rounded-xl bg-cyan-950/40 border border-cyan-500/30 flex flex-col sm:flex-row items-center justify-between gap-2">
        <div class="flex items-center gap-2.5">
          <span class="relative flex h-3 w-3">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75"></span>
            <span class="relative inline-flex rounded-full h-3 w-3 bg-cyan-500"></span>
          </span>
          <span class="text-xs text-cyan-200 font-medium">
            Atomic Redis Lock active for <strong>${lockedSeats.length}</strong> selected berth(s). Secure before timer runs out:
          </span>
        </div>

        <div class="flex items-center gap-2">
          <div class="px-3 py-1 rounded-lg bg-slate-900 border border-cyan-500/40 font-mono font-extrabold text-sm text-cyan-300 flex items-center gap-1.5 shadow-inner">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-cyan-400 animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            <span id="lock-countdown-display">05:00</span>
          </div>
        </div>
      </div>

      <!-- Legend -->
      <div class="flex flex-wrap items-center gap-4 py-3 text-xs text-slate-400 border-b border-slate-800/80">
        <div class="flex items-center gap-1.5">
          <div class="w-3.5 h-3.5 rounded bg-emerald-500/20 border border-emerald-500/60 text-emerald-400"></div>
          <span>Available</span>
        </div>
        <div class="flex items-center gap-1.5">
          <div class="w-3.5 h-3.5 rounded bg-cyan-500/30 border-2 border-cyan-400 text-cyan-200"></div>
          <span>Selected by You (Locked)</span>
        </div>
        <div class="flex items-center gap-1.5">
          <div class="w-3.5 h-3.5 rounded bg-amber-500/20 border border-amber-500/60 text-amber-400"></div>
          <span>Locked by Another</span>
        </div>
        <div class="flex items-center gap-1.5">
          <div class="w-3.5 h-3.5 rounded bg-slate-900 border border-slate-800 text-slate-600 opacity-60"></div>
          <span>Booked / Reserved</span>
        </div>
      </div>

      <!-- Seat Map Content Area (Scrollable) -->
      <div id="coach-grid-container" class="flex-1 overflow-y-auto py-4 space-y-6 pr-1 custom-scrollbar">
        <div class="text-center py-12 text-slate-500">
          <svg class="animate-spin h-7 w-7 text-cyan-400 mx-auto mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span class="text-xs">Fetching real-time coach seat layout & Redis locks...</span>
        </div>
      </div>

      <!-- Footer Action -->
      <div class="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div class="text-xs text-slate-400">
          <span>Selected: </span>
          <span id="selected-seats-summary" class="font-mono font-bold text-white">
            ${lockedSeats.length > 0 ? lockedSeats.map(s => `${s.coachNumber}-${s.seatNumber} (${s.berthType})`).join(', ') : 'None (Auto-allocation available)'}
          </span>
          <span class="text-slate-500 ml-2">Total: ₹${(lockedSeats.length || 1) * (fare || 0)}</span>
        </div>

        <div class="flex items-center gap-3 w-full sm:w-auto">
          <button id="skip-to-booking-btn" class="flex-1 sm:flex-none px-4 py-2.5 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-850 text-slate-300 border border-slate-700 transition">
            Auto-Allocate Berths
          </button>
          <button id="proceed-to-checkout-btn" class="flex-1 sm:flex-none px-6 py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 shadow-lg shadow-cyan-500/25 transition flex items-center justify-center gap-1.5">
            <span>Proceed to Passenger Details & Payment →</span>
          </button>
        </div>
      </div>

    </div>
  `;

  container.appendChild(modalWrapper);

  // Start timer if there are active locked seats
  if (lockedSeats.length > 0 && !countdownInterval) {
    startCountdown();
  }

  // Load seats from backend
  loadCoachSeats();

  async function loadCoachSeats() {
    const gridContainer = modalWrapper.querySelector('#coach-grid-container');
    try {
      const seats = await InventoryApi.getSeatMap(trainNumber, journeyDate, classCode);

      if (!seats || seats.length === 0) {
        gridContainer.innerHTML = `
          <div class="p-8 text-center glass-panel rounded-xl">
            <p class="text-slate-400 text-sm">No physical coaches found for class ${classCode}.</p>
            <p class="text-xs text-slate-500 mt-1">You may proceed with automatic booking allocation.</p>
          </div>
        `;
        return;
      }

      // Group seats by coachNumber
      const coaches = {};
      seats.forEach(s => {
        if (!coaches[s.coachNumber]) coaches[s.coachNumber] = [];
        coaches[s.coachNumber].push(s);
      });

      gridContainer.innerHTML = `
        <div class="space-y-6">
          ${Object.entries(coaches).map(([coachNum, coachSeats]) => `
            <div class="p-4 rounded-xl bg-slate-950/80 border border-slate-800">
              <div class="flex items-center justify-between pb-3 mb-3 border-b border-slate-900">
                <div class="flex items-center gap-2">
                  <span class="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-700 font-mono text-xs font-bold text-cyan-400">
                    Coach ${coachNum}
                  </span>
                  <span class="text-xs text-slate-400">${coachSeats.length} Berths</span>
                </div>
                <span class="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Railway Coupe Layout</span>
              </div>

              <!-- Berth Grid -->
              <div class="grid grid-cols-4 sm:grid-cols-6 md:grid-cols-8 gap-2.5">
                ${coachSeats.map(seat => {
                  const isSelected = store.getState().lockedSeats.some(
                    s => s.coachNumber === seat.coachNumber && s.seatNumber === seat.seatNumber
                  );
                  const isLockedByOther = seat.effectiveStatus === 'LOCKED' && !seat.lockedByCurrentUser && !isSelected;
                  const isBooked = seat.effectiveStatus === 'BOOKED';
                  const isAvailable = !isBooked && !isLockedByOther && !isSelected;

                  let styleClasses = '';
                  let statusLabel = '';
                  let canClick = false;

                  if (isSelected) {
                    styleClasses = 'bg-cyan-500/25 border-cyan-400 text-cyan-200 ring-2 ring-cyan-400 shadow-md shadow-cyan-500/30 scale-102';
                    statusLabel = 'Selected';
                    canClick = true;
                  } else if (isAvailable) {
                    styleClasses = 'bg-emerald-950/30 border-emerald-500/40 text-emerald-300 hover:bg-emerald-500/20 hover:border-emerald-400 cursor-pointer hover:scale-105';
                    statusLabel = seat.berthType;
                    canClick = true;
                  } else if (isLockedByOther) {
                    styleClasses = 'bg-amber-950/30 border-amber-500/40 text-amber-400 cursor-not-allowed opacity-70';
                    statusLabel = 'Locked';
                  } else {
                    styleClasses = 'bg-slate-900 border-slate-800 text-slate-600 cursor-not-allowed opacity-40';
                    statusLabel = 'Booked';
                  }

                  return `
                    <button 
                      data-coach="${seat.coachNumber}" 
                      data-seat="${seat.seatNumber}" 
                      data-berth="${seat.berthType}"
                      data-available="${canClick}"
                      data-selected="${isSelected}"
                      ${!canClick ? 'disabled' : ''}
                      class="berth-btn relative p-2 rounded-xl border flex flex-col items-center justify-between text-center transition-all ${styleClasses} min-h-[64px]"
                    >
                      <div class="w-full flex items-center justify-between text-[10px] font-mono opacity-80">
                        <span>#${seat.seatNumber}</span>
                        ${isSelected ? '<span class="text-cyan-300">✓</span>' : ''}
                      </div>

                      <div class="my-0.5 font-bold font-mono text-xs">
                        ${seat.coachNumber}-${seat.seatNumber}
                      </div>

                      <span class="text-[9px] font-medium tracking-tight uppercase">
                        ${statusLabel}
                      </span>
                    </button>
                  `;
                }).join('')}
              </div>
            </div>
          `).join('')}
        </div>
      `;

      // Attach seat click events
      gridContainer.querySelectorAll('.berth-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
          const coach = btn.getAttribute('data-coach');
          const seatNum = parseInt(btn.getAttribute('data-seat'), 10);
          const berthType = btn.getAttribute('data-berth');
          const isSelected = btn.getAttribute('data-selected') === 'true';

          if (isSelected) {
            // Unlock seat
            try {
              await InventoryApi.unlockSeat(trainNumber, journeyDate, classCode, coach, seatNum);
              store.removeLockedSeat(coach, seatNum);
              if (store.getState().lockedSeats.length === 0) {
                stopCountdown();
              }
              loadCoachSeats();
              updateSummary();
            } catch (err) {
              alert(`Failed to release seat: ${err.message}`);
            }
          } else {
            // Lock seat
            const currentCount = store.getState().lockedSeats.length;
            const maxAllowed = selectedQuota === 'TATKAL' ? 4 : 6;
            if (currentCount >= maxAllowed) {
              alert(`Maximum ${maxAllowed} seats allowed per booking under ${selectedQuota} quota.`);
              return;
            }

            try {
              const res = await InventoryApi.lockSeat(trainNumber, journeyDate, classCode, coach, seatNum);
              store.addLockedSeat({
                trainNumber,
                journeyDate,
                classCode,
                coachNumber: coach,
                seatNumber: seatNum,
                berthType,
                expiresAt: res.expiresAt,
              });

              if (!countdownInterval) {
                remainingSeconds = res.ttlSeconds || 300;
                startCountdown();
              }

              loadCoachSeats();
              updateSummary();
            } catch (err) {
              alert(`Could not lock berth: ${err.message}`);
              loadCoachSeats();
            }
          }
        });
      });

    } catch (err) {
      gridContainer.innerHTML = `
        <div class="p-6 rounded-xl bg-red-500/10 border border-red-500/30 text-center">
          <p class="text-xs text-red-400 font-bold">${err.message || 'Failed to retrieve coach seats'}</p>
        </div>
      `;
    }
  }

  function updateSummary() {
    const locked = store.getState().lockedSeats;
    const banner = modalWrapper.querySelector('#redis-lock-banner');
    const summary = modalWrapper.querySelector('#selected-seats-summary');

    if (banner) {
      if (locked.length > 0) banner.classList.remove('hidden');
      else banner.classList.add('hidden');
    }

    if (summary) {
      summary.textContent = locked.length > 0
        ? locked.map(s => `${s.coachNumber}-${s.seatNumber} (${s.berthType})`).join(', ')
        : 'None (Auto-allocation available)';
    }
  }

  function startCountdown() {
    stopCountdown();
    remainingSeconds = remainingSeconds > 0 ? remainingSeconds : 300;
    updateTimerDisplay();

    countdownInterval = setInterval(async () => {
      remainingSeconds--;
      updateTimerDisplay();

      if (remainingSeconds <= 0) {
        stopCountdown();
        alert('Redis seat lock timer expired! Releasing seats back to the general pool.');
        await releaseAllLocks();
        store.clearLockedSeats();
        loadCoachSeats();
        updateSummary();
      }
    }, 1000);
  }

  function stopCountdown() {
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }
  }

  function updateTimerDisplay() {
    const el = modalWrapper.querySelector('#lock-countdown-display');
    if (!el) return;
    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    el.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  async function releaseAllLocks() {
    const seatsToRelease = [...store.getState().lockedSeats];
    for (const s of seatsToRelease) {
      try {
        await InventoryApi.unlockSeat(s.trainNumber, s.journeyDate, s.classCode, s.coachNumber, s.seatNumber);
      } catch {
        // ignore on release
      }
    }
    store.clearLockedSeats();
    stopCountdown();
  }

  // Close modal handler (unlocks seats)
  modalWrapper.querySelector('#close-coach-picker-btn')?.addEventListener('click', async () => {
    if (store.getState().lockedSeats.length > 0) {
      if (confirm('Closing this window will release your locked berths. Do you wish to continue?')) {
        await releaseAllLocks();
        store.closeModal();
      }
    } else {
      store.closeModal();
    }
  });

  // Skip to booking with auto-allocation
  modalWrapper.querySelector('#skip-to-booking-btn')?.addEventListener('click', () => {
    store.openBookingModal({
      ...activeCoachPicker,
      selectedSeats: [],
      quotaType: selectedQuota,
    });
  });

  // Proceed to booking checkout with locked seats
  modalWrapper.querySelector('#proceed-to-checkout-btn')?.addEventListener('click', () => {
    const locked = store.getState().lockedSeats;
    store.openBookingModal({
      ...activeCoachPicker,
      selectedSeats: locked,
      quotaType: selectedQuota,
    });
  });
}
