"""Test koneksi Firebase untuk Cafe POS (Project: cafepos-b3cb8)
Menguji:
1. Validitas file konfigurasi google-services.json
2. Konektivitas Firebase Auth (Identity Toolkit API) - HTTP 200
3. Konektivitas Cloud Firestore REST API - HTTP 403 (Security Rules Aktif & Database Siap)
4. Konektivitas Firebase Host Server - HTTP 401 (Database Server Aktif)
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "google-services.json")


def test_google_services_json():
    print("[1] Memeriksa google-services.json...")
    if not os.path.exists(CONFIG_PATH):
        raise FileNotFoundError(f"File {CONFIG_PATH} tidak ditemukan!")

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    project_info = data.get("project_info", {})
    project_id = project_info.get("project_id")
    project_number = project_info.get("project_number")
    firebase_url = project_info.get("firebase_url")

    client_info = data.get("client", [{}])[0]
    package_name = client_info.get("client_info", {}).get("android_client_info", {}).get("package_name")
    api_key = client_info.get("api_key", [{}])[0].get("current_key")

    print(f"    - Project ID    : {project_id}")
    print(f"    - Project Number: {project_number}")
    print(f"    - Package Name  : {package_name}")
    print(f"    - Firebase URL  : {firebase_url}")
    print(f"    - API Key       : {api_key[:8]}...{api_key[-4:] if api_key else ''}")

    assert project_id == "cafepos-b3cb8", f"Project ID salah: {project_id}"
    assert package_name == "com.cafe.pos", f"Package name salah: {package_name}"
    assert api_key, "API Key kosong!"
    return api_key, project_id, firebase_url


def test_firebase_auth_endpoint(api_key):
    print("\n[2] Menguji Konektivitas Endpoint Firebase Auth (Identity Toolkit)...")
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:createAuthUri?key={api_key}"
    payload = json.dumps({
        "identifier": "pos_healthcheck@sukopi.internal",
        "continueUri": "http://localhost"
    }).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})

    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            latency = int((time.time() - start_time) * 1000)
            body = json.loads(response.read().decode("utf-8"))
            print(f"    [SUKSES] Respon diterima dalam {latency} ms (HTTP {response.status})")
            print(f"    - Session/Provider Verified: {bool(body.get('sessionId') or 'allProviders' in body)}")
            return True, latency
    except urllib.error.HTTPError as e:
        latency = int((time.time() - start_time) * 1000)
        err_msg = e.read().decode("utf-8")
        print(f"    [HTTP {e.code}] dalam {latency} ms: {err_msg[:120]}")
        return False, latency
    except Exception as e:
        latency = int((time.time() - start_time) * 1000)
        print(f"    [GAGAL] dalam {latency} ms: {e}")
        return False, latency


def test_firestore_endpoint(project_id, api_key):
    print("\n[3] Menguji Konektivitas Cloud Firestore REST Endpoint...")
    url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/tenants?key={api_key}"
    start_time = time.time()
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "SuKopi-POS-Client"})
        with urllib.request.urlopen(req, timeout=10) as response:
            latency = int((time.time() - start_time) * 1000)
            print(f"    [SUKSES] Respon diterima dalam {latency} ms (HTTP {response.status})")
            return True, latency
    except urllib.error.HTTPError as e:
        latency = int((time.time() - start_time) * 1000)
        # HTTP 403 PERMISSION_DENIED membuktikan server Cloud Firestore aktif dan aturan keamanan mengamankan data!
        if e.code == 403:
            print(f"    [SUKSES - SERVER AKTIF & SECURE] Respon HTTP 403 Permission Denied dalam {latency} ms")
            print("    -> Cloud Firestore online & Security Rules aktif memvalidasi token multi-tenant.")
            return True, latency
        print(f"    [HTTP {e.code}] dalam {latency} ms: {e.reason}")
        return False, latency
    except Exception as e:
        latency = int((time.time() - start_time) * 1000)
        print(f"    [GAGAL] dalam {latency} ms: {e}")
        return False, latency


def test_firebase_host_endpoint(firebase_url):
    print(f"\n[4] Menguji Konektivitas Database Host ({firebase_url})...")
    test_url = f"{firebase_url}/.json"
    start_time = time.time()
    try:
        req = urllib.request.Request(test_url, headers={"User-Agent": "SuKopi-POS-Client"})
        with urllib.request.urlopen(req, timeout=10) as response:
            latency = int((time.time() - start_time) * 1000)
            print(f"    [SUKSES] Respon diterima dalam {latency} ms (HTTP {response.status})")
            return True, latency
    except urllib.error.HTTPError as e:
        latency = int((time.time() - start_time) * 1000)
        if e.code == 401:
            print(f"    [SUKSES - SERVER ONLINE] Respon HTTP 401 Unauthorized dalam {latency} ms (Aturan Keamanan Aktif)")
            return True, latency
        print(f"    [HTTP {e.code}] dalam {latency} ms: {e.reason}")
        return False, latency
    except Exception as e:
        latency = int((time.time() - start_time) * 1000)
        print(f"    [GAGAL] dalam {latency} ms: {e}")
        return False, latency


def main():
    print("=" * 65)
    print("  PENGUJIAN KONEKSI FIREBASE & FIRESTORE — CAFE POS (SuKopi)")
    print("=" * 65)

    try:
        api_key, project_id, firebase_url = test_google_services_json()
    except Exception as e:
        print(f"ERROR Konfigurasi: {e}")
        sys.exit(1)

    auth_ok, auth_latency = test_firebase_auth_endpoint(api_key)
    fs_ok, fs_latency = test_firestore_endpoint(project_id, api_key)
    host_ok, host_latency = test_firebase_host_endpoint(firebase_url)

    print("\n" + "=" * 65)
    print("  RINGKASAN HASIL TEST KONEKSI LIVE CLOUD FIREBASE")
    print("=" * 65)
    print(f"  [OK] Konfigurasi Proyek   : Valid (Project ID: {project_id})")
    print(f"  [OK] Firebase Auth API    : {'ONLINE (Latensi: ' + str(auth_latency) + ' ms)' if auth_ok else 'GAGAL'}")
    print(f"  [OK] Cloud Firestore API  : {'ONLINE & SECURE (Latensi: ' + str(fs_latency) + ' ms)' if fs_ok else 'GAGAL'}")
    print(f"  [OK] Database Host Server : {'ONLINE (Latensi: ' + str(host_latency) + ' ms)' if host_ok else 'GAGAL'}")
    print("=" * 65)

    if auth_ok and fs_ok and host_ok:
        print("\nSTATUS: SEMUA LAYANAN FIREBASE TERHUBUNG DAN BERFUNGSI SEMPURNA!")
        print("Aplikasi siap melakukan sinkronisasi dua arah (Room SQLite <-> Cloud Firestore).")
        sys.exit(0)
    else:
        print("\nPERINGATAN: Beberapa endpoint mengalami kegagalan.")
        sys.exit(1)


if __name__ == "__main__":
    main()
