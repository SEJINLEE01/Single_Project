let isAdmin = false;
  let swapMode = false;   // 스왑 대기 모드
  let swapSeat = null;    // 첫번째로 선택한 좌석
  let currentSeat = null;  // 현재 선택한 좌석

  let adminCredentials = null; // 관리자 계정 저장 

  let total_col = 3 // 총 열 수
  let total_row = 5 // 총 행 수
  let seatsInRow =  2; // 각 행에 있는 좌석 수 (기본값)

  async function loadSeats() {
    const res = await fetch('/seats');
    const data = await res.json();
    if(adminCredentials === null) {
      adminCredentials = data.admin;  // 관리자 정보 저장
    }
    renderSeats(data.seats);
  }

  function AdminChanging() {
        if(!isAdmin){
          document.getElementById('adminId').value = '';
          document.getElementById('adminPw').value = '';
          document.getElementById('adminLoginError').textContent = '';
          document.getElementById('adminLoginModal').classList.add('active'); 
        }
        else{
            isAdmin=false;
            document.body.classList.remove('admin-mode');

            document.getElementById('settingBtn').style.display = 'none'; // 세팅 아이콘 숨기기
            document.getElementById('statsBtn').style.display = 'none'; // 통계 아이콘 숨기기

            loadSeats(); 
            showAlert('🔐 관리자 모드가 비활성화되었습니다.');
        }
    
  }
  function checkAdminLogin() {
      const id = document.getElementById('adminId').value;
      const pw = document.getElementById('adminPw').value;

      if (id === adminCredentials.id && pw === adminCredentials.password) {
          isAdmin = true;
          document.body.classList.add('admin-mode');
          document.getElementById('adminLoginModal').classList.remove('active');

          document.getElementById('settingBtn').style.display = 'inline'; // 세팅 아이콘 띄우기
          document.getElementById('statsBtn').style.display = 'inline'; // 통계 아이콘 띄우기
          loadSeats();
      } else {
          document.getElementById('adminLoginError').textContent = '❌ 아이디 또는 비밀번호가 틀렸습니다.';
      }
  }

  function renderSeats(seats) {
    const layout = document.getElementById('seatLayout');
    layout.innerHTML = '';
    
    let presentCount = 0;
    let assignedCount = 0;

    layout.style.gridTemplateColumns = `repeat(${total_col}, 1fr)`;  // 동적으로 열 맞추기

    // seats는 [{seat, id, name, status}] 형태

    const seatMap = {};
    seats.forEach(s => seatMap[s.seat] = s);

    for (let col = 0; col < total_col; col++) {
        const colDiv = document.createElement('div');
        colDiv.className = 'column';

        for (let row = 0; row < total_row; row++) {
            const rowDiv = document.createElement('div');
            rowDiv.className = 'row';
            rowDiv.style.gridTemplateColumns = `repeat(${seatsInRow}, 1fr)`; // 동적으로 행에 좌석 넣기

            for (let s = 0; s < seatsInRow; s++) {
            const seatNum = (row * total_col * seatsInRow) + (col * seatsInRow) + s + 1;
            const seat = seatMap[seatNum];
            const div = document.createElement('div');

            if (!seat) {
                div.className = 'seat';
                div.innerHTML = `
                <span class="seat-num">${seatNum}</span>
                <span class="seat-icon">🪑</span>
                <div class="seat-name" style="color:#ccc">미배정</div>
                `;

                // 스왑 대기 모드일 때 빈 자리도 클릭 가능
                if (swapMode) {
                    div.style.cursor = 'pointer';
                    div.addEventListener('click', () => {
                        if (confirm(`${swapSeat.name || swapSeat.id} → ${seatNum}번으로 이동할까요?`)) {
                            doSwap({ seat: seatNum });
                        }
                    });
                }
            } else {
                const isPresent = seat.status === 1;
                if (isPresent) presentCount++;
                assignedCount++;
                div.className = `seat ${isPresent ? 'present' : 'empty'}`;
                div.innerHTML = `
                <span class="seat-num">${seatNum}</span>
                <span class="seat-icon">🪑</span>
                <div class="seat-name">${seat.name || seat.id}</div>
                `;
                
                if (swapMode) {
                    if (seat.seat !== swapSeat.seat) {
                        // 다른 자리 클릭 → 스왑
                        div.addEventListener('click', () => {
                            if (confirm(`${swapSeat.name || swapSeat.id} ↔ ${seat.name || seat.id} 자리를 바꿀까요?`)) {
                                doSwap(seat);
                            }
                        });
                    } else {
                        // 자기 자신 좌석 강조
                        div.style.outline = '3px solid #4a90d9';
                    }
                }
                else if (isAdmin) {
                    div.addEventListener('click', () => openModal(seat)); // 이미 받아온 seat 객체 그대로 넘김
                }
            }
            rowDiv.appendChild(div);
            }
            colDiv.appendChild(rowDiv);
        }
        layout.appendChild(colDiv);
    }

    document.getElementById('presentCount').textContent = presentCount;
    document.getElementById('absentCount').textContent = assignedCount - presentCount;
    document.getElementById('totalCount').textContent = assignedCount;
    document.getElementById('lastUpdate').textContent = '마지막 업데이트: ' + new Date().toLocaleTimeString('ko-KR');
  }

  // 관리자 모드 모달 관련
  function openModal(seat) {
    currentSeat = seat; 
    const isPresent = seat.status === 1;

    document.getElementById('modalName').textContent = seat.name || seat.id;
    document.getElementById('modalId').textContent = seat.id;
    document.getElementById('modalSeat').textContent = seat.seat + '번';
    document.getElementById('modalStatus').textContent = isPresent ? '✅ 출석' : '❌ 미출석';

    document.getElementById('memberModal').classList.add('active');
  }

  function closeModal(event) {
    // 모달 오버레이 클릭 시에만 닫기
    if (event.target === document.getElementById('memberModal')) {
      closeModalDirect();
    }
  }

  function closeModalDirect() {
    document.getElementById('memberModal').classList.remove('active');
  }

  // 좌석 옮기기 모드 관련
  function startSwap() {
    swapMode = true;
    swapSeat = currentSeat;   // 첫번째 선택 좌석 저장
    closeModalDirect();
    document.getElementById('swapGuide').style.display = 'block';
    loadSeats();   // 빈 자리도 클릭 가능하게 다시 그리기
  }

  function cancelSwap() {
    swapMode = false;
    swapSeat = null;
    document.getElementById('swapGuide').style.display = 'none';
    loadSeats();
  }

  // 관리자 모달 관련
  function closeAdminLoginModal() {
    document.getElementById('adminLoginModal').classList.remove('active');
  }

  function confirmDelete() {
    closeModalDirect();
    showAlert(`⚠️ ${currentSeat.name || currentSeat.id}을(를) 삭제할까요?`, true);
  }

  async function deleteMember() {
      const res = await fetch(`/delete_member/${currentSeat.id}`, {
          method: 'DELETE'
      });
      const data = await res.json();

      if (data.success) {
          closeAlertModal();
          loadSeats();
          showAlert('✅ 삭제되었습니다.');
      } else {
          showAlert('⚠️ 삭제 실패: ' + data.message);
      }
  }

  function showAlert(message, isConfirm = false) {
      document.getElementById('alertMessage').textContent = message;
      document.getElementById('alertConfirmBtn').style.display = isConfirm ? 'block' : 'none'; // ← 이게 매번 실행되어야 함
      document.getElementById('alertCancelBtn').textContent = isConfirm ? '취소' : '확인';
      document.getElementById('alertModal').classList.add('active');
  }

  function closeAlertModal() {
      document.getElementById('alertModal').classList.remove('active');
  }

  // 좌석 옮기기 함수 
  async function doSwap(targetSeat) {
    const res = await fetch('/seats/swap', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ seat_a: swapSeat.seat, seat_b: targetSeat.seat ?? targetSeat })
    });
    const data = await res.json();

    if (data.success) {
      cancelSwap();
      loadSeats();
    } else {
      alert('⚠️ 좌석 변경 실패: ' + data.message);
    }
  }

  function openSettingModal() {
    document.getElementById('settingCol').value = total_col;
    document.getElementById('settingRow').value = total_row;
    document.getElementById('settingSeatsInRow').value = seatsInRow;
    document.getElementById('settingModal').classList.add('active');
  }

  function closeSettingModal() {
      document.getElementById('settingModal').classList.remove('active');
  }

  function applySettings() {
      total_col = parseInt(document.getElementById('settingCol').value);
      total_row = parseInt(document.getElementById('settingRow').value);
      seatsInRow = parseInt(document.getElementById('settingSeatsInRow').value);
      closeSettingModal();
      loadSeats();
  }

  // 통계 모달 열기
function openStatsModal() {
    document.getElementById('statsModal').classList.add('active');
    loadStudentStats();
}

// 통계 모달 닫기
function closeStatsModal() {
    document.getElementById('statsModal').classList.remove('active');
}

// 학생 출석 통계 불러오기
async function loadStudentStats() {
    try {
        const response = await fetch('/attendance/all');
        const data = await response.json();
        
        if (data.success) {
            displayStudentStats(data.data);
        } else {
            alert('통계를 불러오는데 실패했습니다.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('서버 오류가 발생했습니다.');
    }
}

// 통계 테이블에 표시
function displayStudentStats(students) {
    const tbody = document.getElementById('statsTableBody');
    tbody.innerHTML = '';
    
    if (students.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">데이터가 없습니다.</td></tr>';
        return;
    }
    
    students.forEach(student => {
        const row = `
            <tr>
                <td>${student.device_address}</td>
                <td>${student.attendance_count}</td>
                <td>${student.late_count}</td>
                <td>${student.early_leave_count}</td>
                <td>${student.absence_count}</td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}
  
  document.getElementById('adminPw').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') checkAdminLogin();
  });

  // 5초마다 자동 새로고침
  loadSeats();
  setInterval(loadSeats, 5000); 