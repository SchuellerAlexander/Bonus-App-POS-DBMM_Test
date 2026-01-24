document.addEventListener("DOMContentLoaded", () => {
  const revealItems = document.querySelectorAll(".reveal");
  revealItems.forEach((item, index) => {
    const delay = 80 * index;
    window.setTimeout(() => item.classList.add("is-visible"), delay);
  });

  document.querySelectorAll("form[data-loading]").forEach((form) => {
    form.addEventListener("submit", () => {
      form.classList.add("is-loading");
      form.querySelectorAll("button[type='submit']").forEach((btn) => {
        btn.disabled = true;
      });
    });
  });

  // Profile Modal Functionality
  const profileBtn = document.getElementById("profileBtn");
  const accountModal = document.getElementById("accountModal");
  const closeModalBtn = document.getElementById("closeModalBtn");
  const modalOverlay = document.querySelector(".modal-overlay");
  const accountContent = document.getElementById("accountContent");

  if (profileBtn && accountModal) {
    profileBtn.addEventListener("click", () => {
      // Get current account data from the page or via API
      const loadAccountInfo = () => {
        const accountInput = document.getElementById("accountId");
        const accountIdFromInput = accountInput ? accountInput.value : null;
        const accountIdFromBody = document.body.getAttribute("data-account-id");
        const accountId = accountIdFromInput || accountIdFromBody;

        if (!accountId) {
          accountContent.innerHTML = '<div class="text-ink/60">Please load an account first in the Dashboard</div>';
          accountModal.classList.remove("hidden");
          return;
        }

        // Fetch the account data from the API
        fetch(`/api/accounts/${accountId}?includeLedger=false`)
          .then(response => response.json())
          .then(data => {
            let html = '<div class="space-y-4">';

            // Customer Name
            if (data.firstName && data.lastName) {
              html += `<div><span class="text-ink/60 text-sm">Customer Name</span><div class="font-semibold text-lg mt-1">${data.firstName} ${data.lastName}</div></div>`;
            }

            // Profile ID (Account ID)
            if (data.id) {
              html += `<div><span class="text-ink/60 text-sm">Profile ID</span><div class="font-semibold text-lg mt-1">${data.id}</div></div>`;
            }

            // Account Number
            if (data.accountNumber) {
              html += `<div><span class="text-ink/60 text-sm">Account Number</span><div class="font-semibold text-lg mt-1">${data.accountNumber}</div></div>`;
            }

            // Current Points
            if (data.currentPoints !== undefined) {
              html += `<div><span class="text-ink/60 text-sm">Current Points</span><div class="font-display text-3xl font-bold mt-2 text-accent">${data.currentPoints}</div></div>`;
            }

            // Status and Tier
            if (data.status || data.tier) {
              html += `<div><span class="text-ink/60 text-sm">Status & Tier</span><div class="flex gap-2 mt-2">`;
              if (data.status) {
                html += `<span class="stat-pill">${data.status}</span>`;
              }
              if (data.tier) {
                html += `<span class="stat-pill">${data.tier}</span>`;
              }
              html += `</div></div>`;
            }

            html += '</div>';
            accountContent.innerHTML = html;
          })
          .catch(error => {
            console.error("Error loading account details:", error);
            accountContent.innerHTML = '<div class="text-red-600">Error loading account information</div>';
          });
      };

      loadAccountInfo();
      accountModal.classList.remove("hidden");
    });

    closeModalBtn.addEventListener("click", () => {
      accountModal.classList.add("hidden");
    });

    modalOverlay.addEventListener("click", () => {
      accountModal.classList.add("hidden");
    });

    // Close modal on Escape key
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !accountModal.classList.contains("hidden")) {
        accountModal.classList.add("hidden");
      }
    });
  }

  // Ledger Details Modal
  const ledgerModal = document.getElementById("ledgerModal");
  const ledgerContent = document.getElementById("ledgerContent");
  const closeLedgerModalBtn = document.getElementById("closeLedgerModalBtn");

  if (ledgerModal && ledgerContent) {
    const closeLedgerModal = () => {
      ledgerModal.classList.add("hidden");
    };

    ledgerModal.querySelector(".modal-overlay")?.addEventListener("click", closeLedgerModal);
    closeLedgerModalBtn?.addEventListener("click", closeLedgerModal);
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !ledgerModal.classList.contains("hidden")) {
        closeLedgerModal();
      }
    });

    document.querySelectorAll("[data-ledger-id]").forEach((row) => {
      row.addEventListener("click", () => {
        const ledgerId = row.getAttribute("data-ledger-id");
        if (!ledgerId) {
          return;
        }

        ledgerContent.innerHTML = '<div class="text-ink/60">Loading purchase details...</div>';
        ledgerModal.classList.remove("hidden");

        fetch(`/api/ledger/${ledgerId}/purchase`)
          .then((response) => {
            if (!response.ok) {
              throw new Error("Failed to load purchase details");
            }
            return response.json();
          })
          .then((data) => {
            const safeValue = (value) => (value === null || value === undefined || value === "" ? "-" : value);
            const rows = [
              { label: "Account Id", value: safeValue(data.accountId) },
              { label: "Restaurant Id", value: safeValue(data.restaurantId) },
              { label: "Purchase Number", value: safeValue(data.purchaseNumber) },
              { label: "Total Amount", value: safeValue(data.totalAmount) },
              { label: "Currency", value: safeValue(data.currency) },
              { label: "Notes", value: safeValue(data.notes) },
              { label: "Description", value: safeValue(data.description) }
            ];

            let html = '<div class="space-y-4">';
            rows.forEach((item) => {
              html += `<div><span class="text-ink/60 text-sm">${item.label}</span><div class="font-semibold text-lg mt-1">${item.value}</div></div>`;
            });
            html += "</div>";
            ledgerContent.innerHTML = html;
          })
          .catch(() => {
            ledgerContent.innerHTML = '<div class="text-ink/60">No purchase details available for this ledger entry.</div>';
          });
      });
    });
  }
});
