document.addEventListener('DOMContentLoaded', () => {
    let currentTrips = [];
    let selectedTripId = null;
    let leafletMap = null;
    let mapMarkers = [];

    // Tab Navigation
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(targetTab).classList.add('active');

            if (targetTab === 'tab-map' && !leafletMap) {
                initLeafletMap();
            } else if (targetTab === 'tab-metrics') {
                loadModelMetrics();
            } else if (targetTab === 'tab-audit') {
                loadAuditLogs();
            }
        });
    });

    // Action Buttons
    const btnRefresh = document.getElementById('btn-refresh');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', () => {
            loadDashboardData();
        });
    }

    const btnSim = document.getElementById('btn-run-simulation');
    if (btnSim) {
        btnSim.addEventListener('click', async () => {
            btnSim.disabled = true;
            btnSim.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Simulating...';

            try {
                const res = await fetch('/api/simulate-all', { method: 'POST' });
                const data = await res.json();
                alert(`Auto Simulation Complete!\n- Total Trips: ${data.totalTrips}\n- Auto-Executed: ${data.autoExecuted}\n- Requiring Approval: ${data.requiresApproval}\n- Total Delay Reduction: ${data.totalDelaySavedMin} mins`);
                await loadDashboardData();
            } catch (e) {
                console.error(e);
                alert('Simulation failed: ' + e.message);
            } finally {
                btnSim.disabled = false;
                btnSim.innerHTML = '<i class="fa-solid fa-bolt"></i> Run Auto-Simulation';
            }
        });
    }

    // Filter Risk Level
    const filterRisk = document.getElementById('filter-risk');
    if (filterRisk) {
        filterRisk.addEventListener('change', (e) => {
            renderTripsTable(e.target.value);
        });
    }

    // Initial Data Fetch
    loadDashboardData();

    async function loadDashboardData() {
        try {
            const [summaryRes, predictionsRes] = await Promise.all([
                fetch('/api/system-summary'),
                fetch('/api/predictions')
            ]);

            const summary = await summaryRes.json();
            currentTrips = await predictionsRes.json();

            // Update KPI Dashboard
            document.getElementById('kpi-total-trips').textContent = summary.totalTrips;
            document.getElementById('kpi-risk-trips').textContent = summary.criticalTrips + summary.highRiskTrips;
            document.getElementById('kpi-model-mae').textContent = summary.modelMae + ' min';

            // Count auto executed & total saved
            let autoCount = 0;
            currentTrips.forEach(t => {
                if (t.actionState === 'AUTO_EXECUTED') autoCount++;
            });
            document.getElementById('kpi-auto-executed').textContent = autoCount;
            document.getElementById('kpi-delay-saved').textContent = (summary.totalBaselineDelayMin * 0.45).toFixed(1) + ' min';

            renderTripsTable(document.getElementById('filter-risk').value);

            if (currentTrips.length > 0 && !selectedTripId) {
                inspectTrip(currentTrips[0].tripId);
            }
        } catch (err) {
            console.error('Failed to load dashboard data:', err);
        }
    }

    function renderTripsTable(riskFilter) {
        const tbody = document.getElementById('trips-table-body');
        tbody.innerHTML = '';

        let filtered = currentTrips;
        if (riskFilter === 'CRITICAL') {
            filtered = currentTrips.filter(t => t.riskLevel === 'CRITICAL');
        } else if (riskFilter === 'HIGH') {
            filtered = currentTrips.filter(t => t.riskLevel === 'CRITICAL' || t.riskLevel === 'HIGH');
        } else if (riskFilter === 'MEDIUM') {
            filtered = currentTrips.filter(t => t.riskLevel !== 'LOW');
        }

        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="loading-cell">No trips matching selected risk filter.</td></tr>';
            return;
        }

        filtered.forEach(t => {
            const tr = document.createElement('tr');
            if (t.tripId === selectedTripId) tr.classList.add('selected-row');

            let riskBadgeClass = 'badge-low';
            if (t.riskLevel === 'CRITICAL') riskBadgeClass = 'badge-critical';
            else if (t.riskLevel === 'HIGH') riskBadgeClass = 'badge-high';
            else if (t.riskLevel === 'MEDIUM') riskBadgeClass = 'badge-medium';

            let stateBadge = `<span class="badge badge-pending"><i class="fa-solid fa-clock"></i> Pending</span>`;
            if (t.actionState === 'AUTO_EXECUTED') {
                stateBadge = `<span class="badge badge-auto"><i class="fa-solid fa-robot"></i> Auto-Executed</span>`;
            } else if (t.actionState === 'APPROVED') {
                stateBadge = `<span class="badge badge-auto"><i class="fa-solid fa-check"></i> Approved</span>`;
            } else if (t.actionState === 'REJECTED') {
                stateBadge = `<span class="badge badge-blocked"><i class="fa-solid fa-xmark"></i> Rejected</span>`;
            }

            tr.innerHTML = `
                <td><strong>#${t.rank}</strong></td>
                <td>
                    <div style="font-weight:700; color:#fff;">${t.tripId} (${t.busId})</div>
                    <div style="font-size:0.78rem; color:var(--text-muted);">${t.routeId} • ${t.routeName}</div>
                </td>
                <td><span style="font-family:var(--font-heading); font-size:1.05rem; font-weight:700;">+${t.predictedDelayMinutes} min</span></td>
                <td><span class="badge ${riskBadgeClass}">${t.riskLevel}</span></td>
                <td><strong style="color:var(--primary);">${t.priorityScore}</strong></td>
                <td>${stateBadge}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="inspectTrip('${t.tripId}')">
                        <i class="fa-solid fa-sliders"></i> Inspect
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    window.inspectTrip = async function(tripId) {
        selectedTripId = tripId;
        renderTripsTable(document.getElementById('filter-risk').value);

        const detailContainer = document.getElementById('trip-detail-container');
        detailContainer.innerHTML = '<div class="placeholder-state"><i class="fa-solid fa-spinner fa-spin placeholder-icon"></i><p>Loading recovery details for ' + tripId + '...</p></div>';

        try {
            const res = await fetch(`/api/trip-detail?tripId=${tripId}`);
            const trip = await res.json();
            renderTripDetail(trip);
        } catch (e) {
            console.error(e);
            detailContainer.innerHTML = '<div class="placeholder-state text-danger"><p>Error loading trip details.</p></div>';
        }
    };

    function renderTripDetail(trip) {
        const detailContainer = document.getElementById('trip-detail-container');
        
        let decisionBadge = `<span class="badge badge-pending"><i class="fa-solid fa-triangle-exclamation"></i> REQUIRES APPROVAL</span>`;
        if (trip.executionDecision === 'AUTO_EXECUTE') {
            decisionBadge = `<span class="badge badge-auto"><i class="fa-solid fa-robot"></i> AUTO EXECUTED</span>`;
        } else if (trip.executionDecision === 'BLOCKED') {
            decisionBadge = `<span class="badge badge-blocked"><i class="fa-solid fa-lock"></i> BLOCKED</span>`;
        }

        let approvalControls = '';
        if (trip.actionState === 'PENDING_APPROVAL' && trip.executionDecision === 'REQUIRES_APPROVAL') {
            approvalControls = `
                <div class="approval-btn-group">
                    <button class="btn btn-success" style="flex:1;" onclick="approveTripAction('${trip.tripId}')">
                        <i class="fa-solid fa-check-double"></i> Approve & Apply Intervention
                    </button>
                    <button class="btn btn-danger" style="flex:1;" onclick="rejectTripAction('${trip.tripId}')">
                        <i class="fa-solid fa-xmark"></i> Reject Strategy (Retain Baseline)
                    </button>
                </div>
            `;
        } else if (trip.actionState === 'APPROVED') {
            approvalControls = `<div class="text-success" style="margin-top:0.75rem; font-weight:600;"><i class="fa-solid fa-circle-check"></i> Operator Approved. Recovery Intervention Executed.</div>`;
        } else if (trip.actionState === 'REJECTED') {
            approvalControls = `<div class="text-danger" style="margin-top:0.75rem; font-weight:600;"><i class="fa-solid fa-circle-xmark"></i> Operator Rejected. Baseline Schedule Retained.</div>`;
        }

        let candidatesHtml = '';
        trip.candidates.forEach(c => {
            const isSelected = c.candidateId === trip.selectedCandidate.candidateId;
            const cardClass = isSelected ? 'candidate-card selected-candidate' : (c.feasible ? 'candidate-card' : 'candidate-card infeasible-candidate');
            
            candidatesHtml += `
                <div class="${cardClass}">
                    <div class="candidate-head">
                        <h4>${c.title} ${isSelected ? '<span class="badge badge-auto">Optimiser Pick</span>' : ''}</h4>
                        <span class="badge ${c.feasible ? 'badge-low' : 'badge-blocked'}">${c.feasible ? 'Feasible' : 'Infeasible'}</span>
                    </div>
                    <div class="candidate-details">${c.actionDetails}</div>
                    <div class="candidate-metrics">
                        <span><strong>Est. Delay:</strong> +${c.estimatedPostActionDelayMin} min</span>
                        <span><strong>Saving:</strong> -${c.expectedDelayReductionMin} min</span>
                        <span><strong>Cost:</strong> RM${c.estimatedCostImpact.toFixed(2)}</span>
                        <span><strong>Confidence:</strong> ${(c.confidenceScore * 100).toFixed(0)}%</span>
                    </div>
                    <div style="font-size:0.75rem; color:var(--text-dim);"><em>${c.recommendationRationale}</em></div>
                </div>
            `;
        });

        detailContainer.innerHTML = `
            <div class="trip-header-info">
                <div class="trip-title">
                    <h2>${trip.tripId} — Route ${trip.routeId}</h2>
                    <div class="trip-meta-list">
                        <span><i class="fa-solid fa-bus"></i> Bus: ${trip.busId}</span>
                        <span><i class="fa-solid fa-users"></i> Load: ${trip.passengerLoad}%</span>
                        <span><i class="fa-solid fa-cloud-rain"></i> Weather: ${trip.weatherCondition}</span>
                    </div>
                </div>
                <div><span class="badge badge-${trip.riskLevel.toLowerCase()}">${trip.riskLevel} RISK</span></div>
            </div>

            <div class="prediction-banner">
                <div style="font-size:0.82rem; color:var(--text-muted);">PREDICTIVE DELAY INTELLIGENCE (DETECT & CATEGORISE)</div>
                <div class="pred-grid">
                    <div class="pred-box">
                        <label>Predicted Delay</label>
                        <span style="color:var(--danger);">+${trip.predictedDelayMinutes} min</span>
                    </div>
                    <div class="pred-box">
                        <label>Disruption Prob</label>
                        <span>${(trip.delayProbability * 100).toFixed(0)}%</span>
                    </div>
                    <div class="pred-box">
                        <label>Priority Rank Score</label>
                        <span style="color:var(--primary);">${trip.priorityScore}</span>
                    </div>
                </div>
                <div style="margin-top:0.75rem; font-size:0.82rem; color:var(--text-main); font-weight:500;">
                    <strong>Likely Root Cause:</strong> ${trip.delayCause}
                </div>
                <div style="font-size:0.78rem; color:var(--text-muted); margin-top:0.2rem;">${trip.explanationText}</div>
            </div>

            <div class="candidates-title"><i class="fa-solid fa-wand-magic-sparkles"></i> AI Recovery Candidate Optimization (Feature 2)</div>
            <div class="candidates-list">${candidatesHtml}</div>

            <div class="decision-control-box">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-size:0.88rem; font-weight:700;">EXECUTION POLICY & DECISION (FEATURE 3)</span>
                    ${decisionBadge}
                </div>
                <p style="font-size:0.8rem; color:var(--text-muted); margin-top:0.4rem;">
                    <strong>Selected Strategy:</strong> ${trip.selectedCandidate.title} (Delay Reduction: -${trip.selectedCandidate.expectedDelayReductionMin} min)
                </p>
                ${approvalControls}

                <div style="margin-top:1.25rem; padding-top:1rem; border-top:1px solid var(--border-color);">
                    <div style="font-size:0.82rem; font-weight:700; color:var(--text-main);">RECOVERY IMPACT VERIFICATION (FEATURE 4)</div>
                    <div style="display:flex; justify-content:space-between; font-size:0.8rem; margin-top:0.5rem;">
                        <span>Baseline Delay: +${trip.impact.baselineDelayMin} min</span>
                        <span style="color:var(--success); font-weight:700;">Post-Action Delay: +${trip.impact.postActionDelayMin} min</span>
                        <span>Saving: ${trip.impact.percentageImprovement}%</span>
                    </div>
                    <div class="impact-bar-container">
                        <div class="impact-bar-fill" style="width: ${Math.min(100, trip.impact.percentageImprovement)}%;"></div>
                    </div>
                    <div style="font-size:0.75rem; color:var(--text-muted); margin-top:0.4rem;">
                        <i class="fa-solid fa-circle-info"></i> ${trip.impact.verificationSummary}
                    </div>
                </div>
            </div>
        `;
    }

    window.approveTripAction = async function(tripId) {
        try {
            const res = await fetch(`/api/action/approve?tripId=${tripId}`, { method: 'POST' });
            const data = await res.json();
            
            showSuccessModal(data);
            await loadDashboardData();
            inspectTrip(tripId);
        } catch (e) {
            console.error(e);
            alert('Approval failed: ' + e.message);
        }
    };

    function showSuccessModal(data) {
        let existingModal = document.getElementById('success-action-modal');
        if (existingModal) existingModal.remove();

        let stepsHtml = '';
        if (data.movementSteps && data.movementSteps.length > 0) {
            data.movementSteps.forEach((step, idx) => {
                stepsHtml += `
                    <div class="step-item">
                        <div class="step-num">${idx + 1}</div>
                        <div class="step-text">${step}</div>
                    </div>
                `;
            });
        }

        const modalDiv = document.createElement('div');
        modalDiv.id = 'success-action-modal';
        modalDiv.className = 'modal-backdrop';
        modalDiv.innerHTML = `
            <div class="modal-card glass">
                <div class="modal-header">
                    <div class="modal-title">
                        <i class="fa-solid fa-circle-check icon-emerald" style="font-size:1.8rem;"></i>
                        <div>
                            <h3 style="font-size:1.2rem; font-family:var(--font-heading);">SUCCESSFUL INTERVENTION APPLIED!</h3>
                            <p style="font-size:0.8rem; color:var(--text-muted);">Trip ${data.tripId} (${data.busId}) • Route ${data.routeId}</p>
                        </div>
                    </div>
                    <button class="btn-close" onclick="document.getElementById('success-action-modal').remove()"><i class="fa-solid fa-xmark"></i></button>
                </div>

                <div class="modal-body">
                    <div class="kpi-grid" style="grid-template-columns: repeat(3, 1fr); margin-bottom: 1.25rem;">
                        <div class="pred-box">
                            <label>Delay Reduction</label>
                            <span style="color:var(--success); font-size:1.3rem;">-${data.delayReductionMin} min</span>
                        </div>
                        <div class="pred-box">
                            <label>Improvement</label>
                            <span style="color:var(--primary); font-size:1.3rem;">${data.percentageImprovement}%</span>
                        </div>
                        <div class="pred-box">
                            <label>Outcome Status</label>
                            <span style="color:var(--success); font-size:1.1rem;">${data.outcomeStatus}</span>
                        </div>
                    </div>

                    <div style="background:rgba(15,23,42,0.8); border:1px solid var(--border-color); padding:1rem; border-radius:12px; margin-bottom:1.25rem;">
                        <strong style="font-size:0.88rem; color:var(--text-main); display:block; margin-bottom:0.4rem;">
                            <i class="fa-solid fa-wand-magic-sparkles"></i> Executed Strategy: ${data.strategyTitle}
                        </strong>
                        <p style="font-size:0.8rem; color:var(--text-muted);">${data.verificationSummary}</p>
                    </div>

                    <div style="font-size:0.9rem; font-weight:700; margin-bottom:0.75rem; color:var(--text-main); display:flex; align-items:center; gap:0.5rem;">
                        <i class="fa-solid fa-route icon-blue"></i> HOW TO MOVE — Driver Navigation & Dispatch Steps:
                    </div>

                    <div class="movement-steps-list">
                        ${stepsHtml}
                    </div>
                </div>

                <div class="modal-footer" style="margin-top:1.5rem; text-align:right;">
                    <button class="btn btn-primary glow-btn" onclick="document.getElementById('success-action-modal').remove()">
                        <i class="fa-solid fa-check"></i> Acknowledge & Return to Workbench
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modalDiv);
    }

    window.rejectTripAction = async function(tripId) {
        try {
            await fetch(`/api/action/reject?tripId=${tripId}`, { method: 'POST' });
            await loadDashboardData();
            inspectTrip(tripId);
        } catch (e) {
            console.error(e);
        }
    };

    function initLeafletMap() {
        // Center around Kuala Lumpur (Rapid KL Hub)
        leafletMap = L.map('bus-map').setView([3.1390, 101.6869], 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(leafletMap);

        currentTrips.forEach(trip => {
            // Find lat/lng
            fetch(`/api/trip-detail?tripId=${trip.tripId}`).then(r => r.json()).then(t => {
                if (t.latitude && t.longitude) {
                    const color = t.riskLevel === 'CRITICAL' ? '#8B5CF6' : (t.riskLevel === 'HIGH' ? '#EF4444' : '#10B981');
                    const marker = L.circleMarker([t.latitude, t.longitude], {
                        radius: 10,
                        fillColor: color,
                        color: '#ffffff',
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 0.85
                    }).addTo(leafletMap);

                    marker.bindPopup(`
                        <strong>Trip ${t.tripId} (${t.busId})</strong><br>
                        Route ${t.routeId}: ${t.routeName}<br>
                        Predicted Delay: +${t.predictedDelayMinutes} min (${t.riskLevel} Risk)<br>
                        Cause: ${t.delayCause}<br>
                        <button style="margin-top:6px; cursor:pointer;" onclick="inspectTrip('${t.tripId}')">Inspect Action Plan</button>
                    `);
                    mapMarkers.push(marker);
                }
            });
        });
    }

    async function loadModelMetrics() {
        try {
            const res = await fetch('/api/model-metrics');
            const data = await res.json();
            document.getElementById('eval-mae').textContent = data.mae + ' min';
            document.getElementById('eval-rmse').textContent = data.rmse + ' min';
            document.getElementById('eval-precision').textContent = data.precision;
            document.getElementById('eval-recall').textContent = data.recall;
            document.getElementById('eval-f1').textContent = data.f1Score;

            document.getElementById('cm-tn').textContent = `True Negatives (TN): ${data.tn}`;
            document.getElementById('cm-fp').textContent = `False Positives (FP): ${data.fp}`;
            document.getElementById('cm-fn').textContent = `False Negatives (FN): ${data.fn}`;
            document.getElementById('cm-tp').textContent = `True Positives (TP): ${data.tp}`;
        } catch (e) {
            console.error(e);
        }
    }

    async function loadAuditLogs() {
        const tbody = document.getElementById('audit-table-body');
        try {
            const res = await fetch('/api/audit-logs');
            const logs = await res.json();
            if (logs.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="loading-cell">No audit log records found.</td></tr>';
                return;
            }

            tbody.innerHTML = '';
            logs.forEach(l => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><code>${l.auditId}</code></td>
                    <td>${l.timestamp.replace('T', ' ').substring(0, 19)}</td>
                    <td><strong>${l.tripId}</strong> (${l.routeId})</td>
                    <td>+${l.predictedDelayMin} min</td>
                    <td>${l.selectedStrategy}</td>
                    <td><span class="badge ${l.executionDecision === 'AUTO_EXECUTE' ? 'badge-auto' : 'badge-pending'}">${l.executionDecision}</span></td>
                    <td><strong>${l.actionStatus}</strong></td>
                    <td class="text-success">-${l.delayReductionMin} min (${l.percentageImprovement}%)</td>
                    <td style="font-size:0.75rem; max-width:250px;">${l.decisionRationale}</td>
                `;
                tbody.appendChild(tr);
            });
        } catch (e) {
            console.error(e);
        }
    }

    // AI Assistant Chat Widget Toggle & Messaging
    const aiTrigger = document.getElementById('ai-chat-trigger');
    const aiDrawer = document.getElementById('ai-chat-drawer');
    const aiClose = document.getElementById('ai-chat-close');
    const aiInput = document.getElementById('ai-chat-input');
    const aiSend = document.getElementById('ai-chat-send');
    const aiMessages = document.getElementById('ai-chat-messages');

    if (aiTrigger && aiDrawer && aiClose) {
        aiTrigger.addEventListener('click', () => {
            aiDrawer.classList.toggle('open');
        });

        aiClose.addEventListener('click', () => {
            aiDrawer.classList.remove('open');
        });

        const sendChatMessage = async () => {
            const text = aiInput.value.trim();
            if (!text) return;

            appendChatMessage('user', text);
            aiInput.value = '';

            const loadingBubble = appendChatMessage('ai', '<i class="fa-solid fa-spinner fa-spin"></i> Analyzing with Groq LLaMA-3.3...');

            try {
                const res = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: text })
                });
                const data = await res.json();
                loadingBubble.innerHTML = data.reply ? data.reply.replace(/\n/g, '<br>') : 'Received reply.';
            } catch (e) {
                loadingBubble.innerHTML = 'Error communicating with AI Assistant: ' + e.message;
            }
        };

        if (aiSend) aiSend.addEventListener('click', sendChatMessage);
        if (aiInput) {
            aiInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') sendChatMessage();
            });
        }
    }

    function appendChatMessage(sender, htmlContent) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `chat-msg msg-${sender}`;
        const icon = sender === 'ai' ? 'fa-robot' : 'fa-user';
        msgDiv.innerHTML = `
            <div class="msg-avatar"><i class="fa-solid ${icon}"></i></div>
            <div class="msg-bubble">${htmlContent}</div>
        `;
        aiMessages.appendChild(msgDiv);
        aiMessages.scrollTop = aiMessages.scrollHeight;
        return msgDiv.querySelector('.msg-bubble');
    }

    window.sendQuickPrompt = function(promptText) {
        if (aiDrawer) aiDrawer.classList.add('open');
        if (aiInput) {
            aiInput.value = promptText;
            if (aiSend) aiSend.click();
        }
    };
});
