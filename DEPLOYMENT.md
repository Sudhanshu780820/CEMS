# Production Deployment Guide: College Event Management System (CEMS)

This guide provides end-to-end instructions for deploying the **College Event Management System** to production cloud platforms so that students, organizers, and faculty across different devices, Wi-Fi networks, and locations can access the platform online.

---

## 📋 Table of Contents
1. [Architecture & Deployment Overview](#1-architecture--deployment-overview)
2. [Exact Deployment Sequence](#2-exact-deployment-sequence)
3. [Step 1: Cloud MySQL Database Setup](#3-step-1-cloud-mysql-database-setup)
4. [Step 2: Deploy Spring Boot Backend](#4-step-2-deploy-spring-boot-backend)
5. [Step 3: Deploy React Frontend](#5-step-3-deploy-react-frontend)
6. [Step 4: Configure CORS & Cross-Origin Communication](#6-step-4-configure-cors--cross-origin-communication)
7. [Environment Variables Reference](#7-environment-variables-reference)
8. [Testing & Verification Checklist](#8-testing--verification-checklist)
9. [Troubleshooting Common Cloud Issues](#9-troubleshooting-common-cloud-issues)

---

## 1. Architecture & Deployment Overview

The platform consists of three decoupled layers:

```
┌─────────────────────────────────┐
│     React 19 Frontend SPA       │  Hosted on: Vercel / Netlify / Render Static
│  (Static HTML/JS/CSS on CDN)    │  URL: https://my-college-events.vercel.app
└────────────────┬────────────────┘
                 │ REST API calls (JWT Bearer tokens)
┌────────────────▼────────────────┐
│   Spring Boot 3.3.4 Backend     │  Hosted on: Railway / Render / AWS / Docker
│      (Java 21 REST API)         │  URL: https://college-events-api.up.railway.app
└────────────────┬────────────────┘
                 │ JDBC + SSL Connection Pool
┌────────────────▼────────────────┐
│        Cloud MySQL 8.0          │  Hosted on: Railway / Aiven / AWS RDS
│    (event_management_db)        │  Port: 3306 (SSL encrypted)
└─────────────────────────────────┘
```

---

## 2. Exact Deployment Sequence

Deploying in the correct order prevents build failures and connection timeouts:

1. **Deploy Database First**: Set up the managed cloud MySQL database and obtain its connection URL, username, and password.
2. **Deploy Backend Second**: Deploy the Spring Boot application, supplying the database connection environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). The backend will automatically auto-create tables and seed demo data on first boot.
3. **Deploy Frontend Third**: Deploy the React Vite app on Vercel/Netlify, providing the deployed backend URL as `VITE_API_BASE_URL`.
4. **Update Backend CORS Fourth**: Add your newly generated production frontend domain to the backend's `CORS_ALLOWED_ORIGINS` environment variable so the backend accepts browser requests from your frontend.

---

## 3. Step 1: Cloud MySQL Database Setup

Choose any cloud MySQL provider. Here are two popular, reliable options:

### Option A: Railway (Recommended — Fast & Simple)
1. Go to [railway.app](https://railway.app) and sign in with GitHub.
2. Click **New Project** → **Provision MySQL**.
3. Once provisioned, click the MySQL card and go to the **Variables** or **Connect** tab.
4. Copy the connection parameters:
   - Host: e.g. `junction.proxy.rlwy.net`
   - Port: e.g. `12345`
   - User: `root`
   - Password: `<generated_password>`
   - Database: `railway` (or create `event_management_db`)
5. Construct your JDBC URL:
   ```text
   jdbc:mysql://<HOST>:<PORT>/<DATABASE>?createDatabaseIfNotExist=true&useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
   ```

### Option B: Aiven for MySQL (Free Cloud Tier Available)
1. Go to [aiven.io](https://aiven.io) and create a free MySQL service.
2. Select your cloud provider region (e.g. AWS or GCP near your students).
3. Once running, copy the Service URI:
   ```text
   jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/defaultdb?ssl-mode=REQUIRED&serverTimezone=UTC
   ```

---

## 4. Step 2: Deploy Spring Boot Backend

### Option A: Deploy on Railway (Recommended)
1. In your Railway dashboard, click **+ New** → **GitHub Repo** and select this repository.
2. Click on the newly added service → **Settings**:
   - Set **Root Directory** to: `/backend`
   - Set **Build Command**: Railway automatically detects Maven and builds via Dockerfile or `mvnw`.
3. In the **Variables** tab, add the following Environment Variables:

| Variable Name | Value Description / Example |
|---|---|
| `DB_URL` | `jdbc:mysql://<HOST>:<PORT>/<DB>?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `<cloud_db_user>` |
| `DB_PASSWORD` | `<cloud_db_password>` |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `CORS_ALLOWED_ORIGINS` | Temporarily set to `http://localhost:3000` (we will update this in Step 4) |

4. Go to **Settings** → **Networking** → Click **Generate Domain**.
   - Your backend will receive a public HTTPS URL (e.g. `https://college-events-api.up.railway.app`).
5. Verify backend health by visiting in your browser:
   `https://college-events-api.up.railway.app/api/events`
   You should see JSON output of events!

### Option B: Deploy on Render
1. Go to [render.com](https://render.com) and click **New +** → **Web Service**.
2. Connect your GitHub repository.
3. Configure settings:
   - **Root Directory**: `backend`
   - **Environment**: `Docker` (Render will build `backend/Dockerfile`)
   - **Instance Type**: Free or Starter
4. Under **Environment Variables**, add:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
5. Click **Create Web Service**. Note your Render URL (`https://your-service.onrender.com`).

---

## 5. Step 3: Deploy React Frontend

### Deploy on Vercel (Recommended)
1. Go to [vercel.com](https://vercel.com) and sign in.
2. Click **Add New...** → **Project** and import your repository.
3. Configure the Project:
   - **Root Directory**: Click `Edit` and select `frontend`.
   - **Framework Preset**: Vite (detected automatically).
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Expand **Environment Variables** and add:
   - **Name**: `VITE_API_BASE_URL`
   - **Value**: Your deployed backend URL from Step 2 (e.g. `https://college-events-api.up.railway.app`).
5. Click **Deploy**.
   - Vercel will build the frontend and provide your production URL (e.g. `https://college-events.vercel.app`).
   - The included `vercel.json` ensures that refreshing routes (like `/student/dashboard` or `/login`) works seamlessly without 404 errors.

---

## 6. Step 4: Configure CORS & Cross-Origin Communication

Now that you have your production frontend URL:

1. Return to your **Backend Cloud Dashboard** (Railway / Render).
2. Go to **Variables** / **Environment Variables**.
3. Update `CORS_ALLOWED_ORIGINS` to include your production frontend URL:
   ```text
   CORS_ALLOWED_ORIGINS=https://college-events.vercel.app,http://localhost:3000
   ```
4. Redeploy or restart the backend service (Railway and Render redeploy automatically when environment variables change).

> 💡 **Note**: Do not add a trailing slash to the domain in `CORS_ALLOWED_ORIGINS` (use `https://example.vercel.app`, NOT `https://example.vercel.app/`).

---

## 7. Environment Variables Reference

### Backend (`backend/`)
| Variable | Required? | Default (Local Fallback) | Description |
|---|---|---|---|
| `PORT` | Optional | `8080` | Port assigned by cloud provider (Render/Railway inject this automatically). |
| `DB_URL` | **Required in Prod** | `jdbc:mysql://localhost:3306/event_management_db?...` | Full JDBC connection string to the cloud MySQL instance. |
| `DB_USERNAME` | **Required in Prod** | `root` | Database username. |
| `DB_PASSWORD` | **Required in Prod** | `root` | Database user password. |
| `JWT_SECRET` | Recommended | `404E6352...` | 256-bit secret key used to sign and verify JWT tokens. |
| `JWT_EXPIRATION_MS` | Optional | `86400000` (24h) | Token lifespan in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | **Required in Prod** | `http://localhost:3000,http://localhost:5173` | Comma-separated list of authorized frontend origins. |

### Frontend (`frontend/`)
| Variable | Required? | Default (Local Fallback) | Description |
|---|---|---|---|
| `VITE_API_BASE_URL` | **Required in Prod** | `http://localhost:8080` (or `/api` in dev proxy) | Public HTTPS base URL of your deployed Spring Boot API. |

---

## 8. Testing & Verification Checklist

Once deployed, run this quick 5-minute verification:

1. **Public Event Discovery**:
   - Open your production frontend URL: `https://your-frontend.vercel.app`
   - Confirm that the events catalog loads the pre-seeded campus events without errors.
2. **Admin Login**:
   - Go to `/login` and sign in with `admin@college.edu` / `Admin@123` (or click the quick demo chip).
   - Navigate to **Pending Approvals**, **Manage Venues**, and **Reports & Analytics**.
3. **Student Registration & Eligibility**:
   - Log out and log in as `rahul.cse@college.edu` / `Student@123` (Enrollment ID: `EN2023CSE001`).
   - Register for an eligible event (e.g. Hackathon or Cultural Fiesta).
   - Confirm that remaining seats update and the event appears in **My Registrations**.
4. **Live QR Attendance**:
   - Log in as Organizer (`cs.dept@college.edu` / `Organizer@123`).
   - Open an event attendance session to view the live QR code on screen.
   - Have a student scan or enter the session token to check in.
5. **CSV Roster Export**:
   - Click **Export CSV** on the Organizer Participants page.
   - Verify that the browser downloads `participants-event-<id>.csv` containing proper headers and rows.

---

## 9. Troubleshooting Common Cloud Issues

| Issue | Likely Cause | Solution |
|---|---|---|
| **CORS Error in Browser Console** (`Blocked by CORS policy`) | Frontend origin missing from backend CORS list | Update `CORS_ALLOWED_ORIGINS` on backend to match the exact frontend URL (include `https://`, no trailing slash). |
| **Database Connection Refused** (`CommunicationsException`) | Invalid `DB_URL` or database IP whitelist | Ensure `createDatabaseIfNotExist=true&useSSL=true` is in the JDBC URL. Check if cloud database allows all incoming IPs (`0.0.0.0/0`). |
| **Page Refresh Returns 404** (e.g. on `/login` or `/student/dashboard`) | SPA hosting lacks rewrite rule | Ensure `frontend/vercel.json` (for Vercel) or `frontend/public/_redirects` (for Netlify) is present in the deployment. |
| **Backend Out Of Memory (OOM) on Free Tier** | JVM heap limit too high for 512MB RAM container | Add `JAVA_TOOL_OPTIONS="-XX:+UseG1GC -Xmx350m"` to backend environment variables. |
