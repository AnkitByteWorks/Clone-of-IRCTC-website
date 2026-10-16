import { store } from '../state/store.js';
import { UserApi, BookingApi } from '../api/client.js';

export function renderBookingModal(container) {
  const state = store.getState();
  const { activeModal, activeBookingData, currentUser, selectedQuota } = state;

  if (activeModal !== 'booking' || !activeBookingData) {
    return;
  }

  // Auth gate: if not logged in, prompt AuthModal
  if (!currentUser) {
    store.setAuthTab('login');
    store.openModal('auth');
    return;
  }

  const {
    trainNumber,
    trainName,
    fromStationCode,
    toStationCode,
    journeyDate,
    classCode,
    quotaType = selectedQuota || 'GENERAL',
    fare = 0,
    departureTime = '06:00',
    arrivalTime = '18:15',
    selectedSeats = [],
  } = activeBookingData;

  const maxPassengers = quotaType === 'TATKAL' ? 4 : 6;

  // Initial passengers list: prefill from selected seats if available, else at least 1 empty
  let passengers = [];
  if (selectedSeats && selectedSeats.length > 0) {
    passengers = selectedSeats.map((s, idx) => ({
      passengerName: '',
      age: '',
      gender: 'MALE',
      berthPreference: s.berthType || 'NO_PREFERENCE',
      lockedCoachNumber: s.coachNumber,
      lockedSeatNumber: s.seatNumber,
    }));
  } else {
    passengers = [{
      passengerName: '',
      age: '',
      gender: 'MALE',
      berthPreference: 'NO_PREFERENCE',
      lockedCoachNumber: null,
      lockedSeatNumber: null,
    }];
  }

  const modalWrapper = document.createElement('div');
  modalWrapper.id = 'booking-modal-backdrop';
  modalWrapper.className = 'fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-950/80 backdrop-blur-md overflow-y-auto animate-fade-in';

  modalWrapper.innerHTML = `
    <div class="relative w-full max-w-3xl rounded-2xl glass-panel-elevated border border-slate-700/80 p-5 sm:p-7 shadow-2xl my-6 flex flex-col max-h-[92vh]">
      
      <!-- Modal Header -->
      <div class="flex items-center justify-between pb-4 border-b border-slate-800">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center text-slate-950 shadow-lg shadow-cyan-500/20 font-bold">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="font-extrabold text-white text-base sm:text-lg">Ticket Booking & Checkout</span>
              <span class="px-2 py-0.5 rounded font-mono font-bold text-xs ${quotaType === 'TATKAL' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'}">
                ${quotaType}
              </span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">ACID Inventory Verification • Instant PNR Generation</p>
          </div>
        </div>

        <button id="close-booking-modal-btn" class="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-850 border border-transparent hover:border-slate-700 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- Main Form (Scrollable) -->
      <form id="checkout-form" class="flex-1 overflow-y-auto py-4 space-y-6 pr-1 custom-scrollbar">
        
        <!-- Journey Summary Card -->
        <div class="p-4 rounded-xl bg-slate-950/80 border border-slate-800">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-900">
            <div>
              <span class="font-mono text-xs font-bold text-cyan-400">#${trainNumber}</span>
              <span class="text-sm font-bold text-white ml-1.5">${trainName}</span>
            </div>
            <div class="text-xs text-slate-400 font-mono">
              Class: <strong class="text-white">${classCode}</strong> • Base Fare: <strong class="text-white">₹${fare}</strong> / seat
            </div>
          </div>

          <div class="pt-3 grid grid-cols-3 gap-2 text-center">
            <div>
              <p class="text-xs font-mono font-bold text-white">${departureTime}</p>
              <p class="text-[11px] text-slate-400">${fromStationCode}</p>
            </div>
            <div class="flex flex-col items-center justify-center">
              <span class="text-[10px] text-slate-500 uppercase font-mono">${journeyDate}</span>
              <div class="w-full flex items-center justify-center gap-1 my-1">
                <div class="w-1.5 h-1.5 rounded-full bg-cyan-400"></div>
                <div class="h-0.5 flex-1 bg-slate-700"></div>
                <div class="w-1.5 h-1.5 rounded-full bg-emerald-400"></div>
              </div>
            </div>
            <div>
              <p class="text-xs font-mono font-bold text-white">${arrivalTime}</p>
              <p class="text-[11px] text-slate-400">${toStationCode}</p>
            </div>
          </div>

          ${selectedSeats.length > 0 ? `
            <div class="mt-3 pt-3 border-t border-slate-900 flex items-center gap-2 text-xs">
              <span class="text-slate-400">Locked Berths:</span>
              <div class="flex flex-wrap gap-1.5">
                ${selectedSeats.map(s => `
                  <span class="px-2 py-0.5 rounded bg-cyan-500/15 border border-cyan-500/30 text-cyan-300 font-mono text-[11px] font-bold">
                    ${s.coachNumber}-${s.seatNumber} (${s.berthType})
                  </span>
                `).join('')}
              </div>
            </div>
          ` : ''}
        </div>

        <!-- 1-Click Master Passenger Autofill Section -->
        <div id="master-autofill-section">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 text-cyan-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/>
              </svg>
              1-Click Master Passenger Autofill:
            </span>
            <span class="text-[11px] text-slate-500">Fast Tatkal Auto-fill</span>
          </div>

          <div id="saved-passenger-chips" class="flex flex-wrap gap-2">
            <span class="text-xs text-slate-500 py-1">Loading saved passengers...</span>
          </div>
        </div>

        <!-- Passenger Input List -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <h4 class="text-xs font-semibold text-slate-300 uppercase tracking-wider">
              Passengers (<span id="passenger-count-display">${passengers.length}</span> / ${maxPassengers}):
            </h4>

            <button 
              type="button" 
              id="add-passenger-row-btn" 
              class="px-2.5 py-1 rounded-lg text-xs font-bold text-cyan-400 hover:text-cyan-300 bg-cyan-950/60 hover:bg-cyan-900/60 border border-cyan-800/60 transition flex items-center gap-1"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span>Add Passenger</span>
            </button>
          </div>

          <div id="passenger-rows-container" class="space-y-3">
            <!-- Dynamic passenger rows inserted here -->
          </div>
        </div>

        <!-- Payment Method Selection -->
        <div class="pt-2">
          <label class="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-3">
            Select Payment Method:
          </label>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5" id="payment-methods-grid">
            <label class="payment-option p-3 rounded-xl bg-slate-950 border border-cyan-500/50 flex items-center gap-3 cursor-pointer hover:border-cyan-400 transition">
              <input type="radio" name="paymentMethod" value="UPI" checked class="text-cyan-500 focus:ring-0" />
              <div>
                <p class="text-xs font-bold text-white">UPI (Instant 0% Convenience Fee)</p>
                <p class="text-[10px] text-slate-400">Google Pay • PhonePe • Paytm • BHIM</p>
              </div>
            </label>

            <label class="payment-option p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3 cursor-pointer hover:border-slate-700 transition">
              <input type="radio" name="paymentMethod" value="RAIL_WALLET" class="text-cyan-500 focus:ring-0" />
              <div>
                <p class="text-xs font-bold text-white">RailConnect Wallet</p>
                <p class="text-[10px] text-emerald-400">Instant Refund • Zero Latency</p>
              </div>
            </label>

            <label class="payment-option p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3 cursor-pointer hover:border-slate-700 transition">
              <input type="radio" name="paymentMethod" value="CREDIT_CARD" class="text-cyan-500 focus:ring-0" />
              <div>
                <p class="text-xs font-bold text-white">Credit Card</p>
                <p class="text-[10px] text-slate-400">Visa • Mastercard • RuPay</p>
              </div>
            </label>

            <label class="payment-option p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3 cursor-pointer hover:border-slate-700 transition">
              <input type="radio" name="paymentMethod" value="NET_BANKING" class="text-cyan-500 focus:ring-0" />
              <div>
                <p class="text-xs font-bold text-white">Net Banking</p>
                <p class="text-[10px] text-slate-400">SBI • HDFC • ICICI • Axis & all banks</p>
              </div>
            </label>
          </div>
        </div>

      </form>

      <!-- Bottom Checkout Bar -->
      <div class="pt-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <span class="text-xs text-slate-400">Total Payable Amount:</span>
          <p id="total-fare-display" class="text-2xl font-extrabold text-white font-mono mt-0.5">₹${passengers.length * fare}</p>
        </div>

        <div class="flex items-center gap-3 w-full sm:w-auto">
          <button type="button" id="cancel-checkout-btn" class="flex-1 sm:flex-none px-4 py-2.5 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-850 text-slate-300 border border-slate-700 transition">
            Cancel
          </button>
          <button type="submit" form="checkout-form" id="pay-and-book-btn" class="flex-1 sm:flex-none px-8 py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 shadow-lg shadow-emerald-500/25 transition flex items-center justify-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 stroke-[2.5]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <span>Confirm & Pay Now</span>
          </button>
        </div>
      </div>

    </div>
  `;

  container.appendChild(modalWrapper);

  const rowsContainer = modalWrapper.querySelector('#passenger-rows-container');
  const countDisplay = modalWrapper.querySelector('#passenger-count-display');
  const totalFareDisplay = modalWrapper.querySelector('#total-fare-display');
  const chipsContainer = modalWrapper.querySelector('#saved-passenger-chips');

  // Render Passenger Form Rows
  function renderPassengerRows() {
    countDisplay.textContent = passengers.length;
    totalFareDisplay.textContent = `₹${passengers.length * fare}`;

    rowsContainer.innerHTML = passengers.map((p, idx) => {
      const hasLocked = p.lockedCoachNumber && p.lockedSeatNumber;
      return `
        <div class="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3 relative group">
          <div class="flex items-center justify-between pb-2 border-b border-slate-900">
            <div class="flex items-center gap-2">
              <span class="w-5 h-5 rounded-full bg-slate-900 border border-slate-700 text-[10px] font-mono font-bold flex items-center justify-center text-cyan-400">
                ${idx + 1}
              </span>
              <span class="text-xs font-bold text-white">Passenger ${idx + 1}</span>
              ${hasLocked ? `
                <span class="px-2 py-0.2 rounded font-mono text-[10px] bg-cyan-950 text-cyan-300 border border-cyan-800/60 font-semibold">
                  Locked Berth: ${p.lockedCoachNumber}-${p.lockedSeatNumber}
                </span>
              ` : ''}
            </div>

            ${passengers.length > 1 ? `
              <button type="button" data-remove-idx="${idx}" class="remove-row-btn text-xs text-red-400 hover:text-red-300 transition">
                Remove
              </button>
            ` : ''}
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div class="sm:col-span-2">
              <label class="block text-[11px] font-medium text-slate-400 mb-1">Full Name</label>
              <input 
                type="text" 
                data-idx="${idx}" 
                data-field="passengerName" 
                value="${p.passengerName || ''}" 
                required 
                placeholder="Full name as per Govt ID"
                class="passenger-input w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 font-medium"
              />
            </div>

            <div>
              <label class="block text-[11px] font-medium text-slate-400 mb-1">Age</label>
              <input 
                type="number" 
                data-idx="${idx}" 
                data-field="age" 
                value="${p.age || ''}" 
                min="1" 
                max="125" 
                required 
                placeholder="Age"
                class="passenger-input w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500"
              />
            </div>

            <div>
              <label class="block text-[11px] font-medium text-slate-400 mb-1">Gender</label>
              <select data-idx="${idx}" data-field="gender" class="passenger-input w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-cyan-500">
                <option value="MALE" ${p.gender === 'MALE' ? 'selected' : ''}>Male</option>
                <option value="FEMALE" ${p.gender === 'FEMALE' ? 'selected' : ''}>Female</option>
                <option value="OTHER" ${p.gender === 'OTHER' ? 'selected' : ''}>Other</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-[11px] font-medium text-slate-400 mb-1">Berth Preference</label>
            <select data-idx="${idx}" data-field="berthPreference" class="passenger-input w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white focus:outline-none focus:border-cyan-500">
              <option value="NO_PREFERENCE" ${p.berthPreference === 'NO_PREFERENCE' ? 'selected' : ''}>No Preference</option>
              <option value="LOWER" ${p.berthPreference === 'LOWER' ? 'selected' : ''}>Lower Berth</option>
              <option value="MIDDLE" ${p.berthPreference === 'MIDDLE' ? 'selected' : ''}>Middle Berth</option>
              <option value="UPPER" ${p.berthPreference === 'UPPER' ? 'selected' : ''}>Upper Berth</option>
              <option value="SIDE_LOWER" ${p.berthPreference === 'SIDE_LOWER' ? 'selected' : ''}>Side Lower</option>
              <option value="SIDE_UPPER" ${p.berthPreference === 'SIDE_UPPER' ? 'selected' : ''}>Side Upper</option>
              <option value="WINDOW" ${p.berthPreference === 'WINDOW' ? 'selected' : ''}>Window (CC/EC)</option>
            </select>
          </div>
        </div>
      `;
    }).join('');

    // Attach row input listeners
    rowsContainer.querySelectorAll('.passenger-input').forEach(input => {
      input.addEventListener('input', (e) => {
        const idx = parseInt(e.target.getAttribute('data-idx'), 10);
        const field = e.target.getAttribute('data-field');
        passengers[idx][field] = e.target.value;
      });
      input.addEventListener('change', (e) => {
        const idx = parseInt(e.target.getAttribute('data-idx'), 10);
        const field = e.target.getAttribute('data-field');
        passengers[idx][field] = e.target.value;
      });
    });

    // Attach remove buttons
    rowsContainer.querySelectorAll('.remove-row-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.getAttribute('data-remove-idx'), 10);
        passengers.splice(idx, 1);
        renderPassengerRows();
      });
    });
  }

  // Load Master Passengers for 1-click autofill chips
  async function loadMasterPassengers() {
    try {
      const saved = await UserApi.getSavedPassengers();
      if (!saved || saved.length === 0) {
        chipsContainer.innerHTML = `
          <span class="text-xs text-slate-500">No pre-saved passengers. You can add frequent travelers under Master List.</span>
        `;
        return;
      }

      chipsContainer.innerHTML = saved.map(p => `
        <button 
          type="button" 
          data-saved-name="${p.passengerName}" 
          data-saved-age="${p.age}" 
          data-saved-gender="${p.gender}" 
          data-saved-pref="${p.berthPreference}"
          class="saved-chip px-3 py-1 rounded-lg bg-slate-900 hover:bg-cyan-500/15 border border-slate-800 hover:border-cyan-500/40 text-xs text-slate-300 hover:text-cyan-300 transition flex items-center gap-1.5 shadow-sm"
        >
          <span class="text-cyan-400 font-bold">+</span>
          <span>${p.passengerName} (${p.age}, ${p.gender.slice(0, 1)}, ${p.berthPreference})</span>
        </button>
      `).join('');

      chipsContainer.querySelectorAll('.saved-chip').forEach(chip => {
        chip.addEventListener('click', () => {
          const name = chip.getAttribute('data-saved-name');
          const age = parseInt(chip.getAttribute('data-saved-age'), 10);
          const gender = chip.getAttribute('data-saved-gender');
          const pref = chip.getAttribute('data-saved-pref');

          // Find first empty passenger row or add new if limit allows
          const emptyIdx = passengers.findIndex(p => !p.passengerName || p.passengerName.trim() === '');
          if (emptyIdx !== -1) {
            passengers[emptyIdx].passengerName = name;
            passengers[emptyIdx].age = age;
            passengers[emptyIdx].gender = gender;
            passengers[emptyIdx].berthPreference = pref;
          } else {
            if (passengers.length >= maxPassengers) {
              alert(`Maximum ${maxPassengers} passengers allowed per ${quotaType} booking.`);
              return;
            }
            passengers.push({
              passengerName: name,
              age: age,
              gender: gender,
              berthPreference: pref,
              lockedCoachNumber: null,
              lockedSeatNumber: null,
            });
          }
          renderPassengerRows();
        });
      });

    } catch {
      chipsContainer.innerHTML = `<span class="text-xs text-slate-500">Could not load master passenger list</span>`;
    }
  }

  // Add Row Button
  modalWrapper.querySelector('#add-passenger-row-btn')?.addEventListener('click', () => {
    if (passengers.length >= maxPassengers) {
      alert(`Maximum ${maxPassengers} passengers allowed per ${quotaType} booking.`);
      return;
    }
    passengers.push({
      passengerName: '',
      age: '',
      gender: 'MALE',
      berthPreference: 'NO_PREFERENCE',
      lockedCoachNumber: null,
      lockedSeatNumber: null,
    });
    renderPassengerRows();
  });

  // Handle Close / Cancel
  modalWrapper.querySelector('#close-booking-modal-btn')?.addEventListener('click', () => {
    store.closeModal();
  });

  modalWrapper.querySelector('#cancel-checkout-btn')?.addEventListener('click', () => {
    store.closeModal();
  });

  // Form submission (Book & Pay)
  const form = modalWrapper.querySelector('#checkout-form');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Validate passengers
    for (let i = 0; i < passengers.length; i++) {
      const p = passengers[i];
      if (!p.passengerName || p.passengerName.trim() === '') {
        alert(`Please enter a valid legal name for Passenger ${i + 1}`);
        return;
      }
      const ageNum = parseInt(p.age, 10);
      if (isNaN(ageNum) || ageNum < 1 || ageNum > 125) {
        alert(`Please enter a valid age (1-125) for Passenger ${i + 1}`);
        return;
      }
    }

    const paymentMethod = form.paymentMethod.value;
    const submitBtn = modalWrapper.querySelector('#pay-and-book-btn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = `
      <svg class="animate-spin h-4 w-4 text-slate-950" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
      <span>Processing ACID Allocation...</span>
    `;

    const requestPayload = {
      trainNumber,
      fromStationCode,
      toStationCode,
      journeyDate,
      classCode,
      quotaType,
      paymentMethod,
      passengers: passengers.map(p => ({
        passengerName: p.passengerName.trim(),
        age: parseInt(p.age, 10),
        gender: p.gender,
        berthPreference: p.berthPreference,
        lockedCoachNumber: p.lockedCoachNumber || null,
        lockedSeatNumber: p.lockedSeatNumber ? parseInt(p.lockedSeatNumber, 10) : null,
      })),
    };

    try {
      const bookingResponse = await BookingApi.createBooking(requestPayload);

      // Clear locks
      store.clearLockedSeats();

      // Open PNR ticket modal
      store.openTicketModal(bookingResponse);

    } catch (err) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Confirm & Pay Now</span>`;
      alert(`Booking Failed: ${err.message}`);
    }
  });

  // Initial render
  renderPassengerRows();
  loadMasterPassengers();
}
