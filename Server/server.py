from fastapi import FastAPI
from pydantic import BaseModel
from datetime import datetime
from fastapi.responses import HTMLResponse
import sqlite3
import os

app = FastAPI()

# DB 초기화
def init_db():
    if os.path.exists("Data.db"):
        os.remove("Data.db")

    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    
    # 로그 저장 테이블
    cursor.execute("""
        CREATE TABLE Log (
            id INTEGER PRIMARY KEY,
            device_address TEXT,
            action TEXT,
            timestamp TEXT,
            FOREIGN KEY (device_address) REFERENCES Login(device_address)
        )
    """)

    # 회원 정보 담는 테이블
    cursor.execute("""
        CREATE TABLE Login (
            id TEXT,
            password TEXT,
            device_name TEXT,
            device_address TEXT,
            seat INTEGER,
            timestamp TEXT,
            PRIMARY KEY (id)
        )
    """)
    
    # 출석 & 퇴실 체크 테이블
    # 0 -> 퇴실상태, 1 -> 출석상태
    cursor.execute("""
        CREATE TABLE AttendanceStatus (
            device_address TEXT PRIMARY KEY,
            status INTEGER DEFAULT 0,
            FOREIGN KEY (device_address) REFERENCES Login(device_address)
        )
    """)

    # 관리자 계정 생성
    cursor.execute("INSERT INTO Login (id, password) VALUES (?, ?)", ("admin", "1234"))
    
    conn.commit()
    conn.close()

init_db()

# 출석 데이터 모델
class LogData(BaseModel):
    action: str
    device_address: str
    
# 폰 데이터 모델
class PhoneData(BaseModel):
    id: str
    password: str
    device_name: str
    device_address: str
    seat: int

# 로그인시 확인하는 데이터
class LoginData(BaseModel):
    id: str
    password: str

# 비콘스캔 성공시 받는 데이터 
class CheckData(BaseModel):
    device_address: str

# 대시보드 제공
@app.get("/", response_class=HTMLResponse)
def dashboard():
    with open("dashboard.html", "r", encoding="utf-8") as f:
        return f.read()

# 로그 저장 API
@app.post("/attendance")
def save_attendance(data: LogData):
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO Log (action, device_address, timestamp)
        VALUES (?, ?, ?)
    """, (data.action ,data.device_address, datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    conn.commit()
    conn.close()
    return {"status": data.action, "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}


# 폰 등록 API(회원가입)
# 성공 -> 1, 기기 중복 -> 2, id 중복 -> 3
@app.post("/Add_Device")
def ADD_Device(data: PhoneData):
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()

    # 같은 기기 중복 확인
    cursor.execute("SELECT * FROM Login WHERE device_address=?", (data.device_address,))
    result = cursor.fetchone()
    if result:
        return {"success": 2}

    # id 중복 확인
    cursor.execute("SELECT * FROM Login WHERE id=?", (data.id,))
    result = cursor.fetchone()
    if result:
        return {"success": 3}

    # 좌석번호 확인
    cursor.execute("SELECT * FROM Login WHERE seat=?", (data.seat,))
    result = cursor.fetchone()
    if result:
        return {"success": 4}
    
    cursor.execute("""
        INSERT OR IGNORE INTO Login (id, password, device_address, device_name, seat, timestamp)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (data.id, data.password, data.device_address, data.device_name, data.seat, datetime.now().strftime("%Y-%m-%d %H:%M:%S")))

    # 출석 테이블에도 추가
    cursor.execute("""
        INSERT OR IGNORE INTO AttendanceStatus (device_address)
        VALUES (?)
    """, (data.device_address,))
    
    conn.commit()
    conn.close()
    return {"success": 1}

# 로그인시 존재하는지 확인
# 성공시 True, 실패시 False
@app.post("/login")
def login(data: LoginData):
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM Login WHERE id=? AND password=?",
                  (data.id, data.password))
    result = cursor.fetchone()
    conn.close()
    if result:
        return {"success": True}
    else:
        return {"success": False}

# 비콘스캔 성공시 출석인지 퇴실인지 확인
# 0 -> 퇴실상태, 1 -> 출석상태
@app.post("/check")
def check(data: CheckData):
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    cursor.execute("SELECT status FROM AttendanceStatus WHERE device_address=?", (data.device_address,))
    result = cursor.fetchone() # 튜플형태로 저장됨 (0,) 또는 (1,)
    if result[0] == 0: # 출석
        cursor.execute("UPDATE AttendanceStatus SET status=1 WHERE device_address=?", (data.device_address,))
    elif result[0] == 1: # 퇴실
        cursor.execute("UPDATE AttendanceStatus SET status=0 WHERE device_address=?", (data.device_address,))

    conn.commit()
    conn.close()
    
    return {"success": True}

@app.post("/status")
def get_status(data: CheckData):
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    cursor.execute("SELECT status FROM AttendanceStatus WHERE device_address=?", (data.device_address,))
    result = cursor.fetchone()
    conn.close()
    if result is None: # 등록된 기기가 없다면
        return {"status": -1}
    return {"status": result[0]}

# 좌석 정보 조회 API
@app.get("/seats")
def get_seats():
    conn = sqlite3.connect("Data.db")
    cursor = conn.cursor()
    cursor.execute("""
        SELECT l.id, l.device_address, a.status, l.seat
        FROM Login l
        LEFT JOIN AttendanceStatus a ON l.device_address = a.device_address
    """)
    rows = cursor.fetchall()
    conn.close()
    seats = [{"id": r[0], "device_address": r[1], "status": r[2] or 0, "seat": r[3]} for r in rows]
    return {"seats": seats}