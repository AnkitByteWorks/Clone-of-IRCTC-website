import { store } from '../state/store.js';
import { NotificationApi } from '../api/client.js';
import { formatPnr } from './PnrTicketCard.js';

export function renderNotificationDrawer(container) {
  const state = store.getState();
  const { notificationDrawerOpen, notifications, currentUser } = state;

  if (!notificationDrawerOpen || !currentUser) {
    return;
  }

  const unreadCount = notifications.filter(n => !n.readStatus).length;

  const drawerWrapper = document.createElement('div');
  drawerWrapper.id = 'notification-drawer-backdrop';
  drawerWrapper.className = 'fixed inset-0 z-50 flex justify-end bg-slate-950/60 backdrop-blur-sm animate-fade-in';

  drawerWrapper.innerHTML = `
    <div class="relative w-full max-w-md h-full bg-slate-950 border-l border-slate-800 shadow-2xl flex flex-col animate-slide-left">
      
      <!-- Drawer Header -->
      <div class="p-5 border-b border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400 font-bold">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h3 class="font-bold text-white text-base">Kafka Notifications</h3>
              ${unreadCount > 0 ? `
                <span class="px-2 py-0.2 rounded-full text-[10px] font-bold bg-cyan-500 text-slate-950 font-mono">
                  ${unreadCount} new
                </span>
              ` : ''}
            </div>
            <p class="text-[11px] text-slate-400">Real-time booking events & seat promotions</p>
          </div>
        </div>

        <button id="close-drawer-btn" class="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-900 border border-transparent hover:border-slate-800 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- Notifications List (Scrollable) -->
      <div class="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar" id="drawer-notifs-list">
        ${notifications.length === 0 ? `
          <div class="text-center py-16 text-slate-500">
            <div class="w-12 h-12 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-600 mx-auto mb-3">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
            </div>
            <p class="text-sm font-semibold text-slate-400">No notifications yet</p>
            <p class="text-xs text-slate-600 mt-1">Booking confirmations and waitlist auto-promotions will appear here.</p>
          </div>
        ` : notifications.map(n => {
          const isUnread = !n.readStatus;
          let iconHtml = '';
          let typeColorClass = '';
          let badgeText = n.eventType;

          switch (n.eventType) {
            case 'BOOKING_CONFIRMED':
              typeColorClass = 'border-emerald-500/30 bg-emerald-950/20';
              badgeText = 'Booking Confirmed';
              iconHtml = `
                <div class="w-8 h-8 rounded-lg bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 shrink-0">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                </div>
              `;
              break;
            case 'BOOKING_CANCELLED':
              typeColorClass = 'border-amber-500/30 bg-amber-950/20';
              badgeText = 'Ticket Cancelled';
              iconHtml = `
                <div class="w-8 h-8 rounded-lg bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 shrink-0">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                </div>
              `;
              break;
            case 'PASSENGER_PROMOTED':
              typeColorClass = 'border-purple-500/40 bg-purple-950/25';
              badgeText = 'Seat Promoted ★';
              iconHtml = `
                <div class="w-8 h-8 rounded-lg bg-purple-500/20 border border-purple-500/50 flex items-center justify-center text-purple-300 shrink-0 shadow-lg shadow-purple-500/20">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-purple-400 stroke-none" viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                </div>
              `;
              break;
            default:
              typeColorClass = 'border-slate-800 bg-slate-900/40';
              iconHtml = `
                <div class="w-8 h-8 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 shrink-0">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>
                </div>
              `;
          }

          const timeString = n.occurredAt ? new Date(n.occurredAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';

          return `
            <div 
              data-notif-id="${n.id}" 
              class="notif-item p-3.5 rounded-xl border ${typeColorClass} ${isUnread ? 'ring-1 ring-cyan-500/40' : 'opacity-85'} hover:scale-101 transition-all cursor-pointer flex gap-3"
            >
              ${iconHtml}

              <div class="flex-1">
                <div class="flex items-center justify-between gap-1 mb-1">
                  <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono">${badgeText}</span>
                  <span class="text-[10px] text-slate-500 font-mono">${timeString}</span>
                </div>

                <h4 class="text-xs font-bold text-white mb-0.5">${n.subject}</h4>
                <p class="text-[11px] text-slate-300 leading-relaxed">${n.message}</p>

                ${n.pnrNumber ? `
                  <div class="mt-2 flex items-center gap-2">
                    <span class="px-2 py-0.2 rounded font-mono text-[10px] font-bold bg-slate-950 border border-slate-800 text-cyan-400">
                      PNR: ${formatPnr(n.pnrNumber)}
                    </span>
                    ${isUnread ? '<span class="text-[10px] text-cyan-400 font-bold ml-auto">• Tap to mark read</span>' : ''}
                  </div>
                ` : ''}
              </div>
            </div>
          `;
        }).join('')}
      </div>

      <!-- Drawer Footer -->
      <div class="p-4 border-t border-slate-800 bg-slate-950 text-center">
        <span class="text-[10px] text-slate-500 font-mono">
          Powered by Apache Kafka Event Pipeline (Topic: railconnect.booking.events)
        </span>
      </div>

    </div>
  `;

  container.appendChild(drawerWrapper);

  // Close drawer listeners
  drawerWrapper.querySelector('#close-drawer-btn')?.addEventListener('click', () => {
    store.toggleNotificationDrawer(false);
  });

  drawerWrapper.addEventListener('click', (e) => {
    if (e.target === drawerWrapper) {
      store.toggleNotificationDrawer(false);
    }
  });

  // Tap to mark as read
  drawerWrapper.querySelectorAll('.notif-item').forEach(item => {
    item.addEventListener('click', async () => {
      const id = item.getAttribute('data-notif-id');
      try {
        await NotificationApi.markAsRead(id);
        const updated = store.getState().notifications.map(n => n.id === id ? { ...n, readStatus: true } : n);
        store.setNotifications(updated);
      } catch (err) {
        console.warn('Could not mark notification read:', err);
      }
    });
  });
}
