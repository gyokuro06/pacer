# ルーム状態の永続化

// Slice 4: Vercel の multi-instance / cold start でも作成済みルームが残ること。
// ブラウザ E2E では「別 Node プロセス」を安定再現しづらいため、本スライスの受け入れゲートは
// web 契約テスト `web/src/lib/room-store.persist.test.ts`（プロセスヒープ相当の Map を破棄しても getRoom できること）。
// Green は env で切替可能な共有ストアを想定（ローカル無料寄り: SQLite/file、本番: Neon / Vercel KV 等）。

## プロセスメモリを失っても作成済みルームを取得できる
* プロセス再起動後もルームが取得できる契約が満たされている
