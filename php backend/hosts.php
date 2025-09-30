<?php
header('Content-Type: application/json; charset=utf-8');
include 'db.php';

$user_id = $_GET['user_id'] ?? null;
if (!$user_id) {
    echo json_encode(['success' => false, 'message' => 'user_id обязателен']);
    exit;
}

$stmt = $pdo->prepare("SELECT id, name, address, os_type FROM hosts WHERE user_id = ? ORDER BY created_at DESC");
$stmt->execute([$user_id]);
$hosts = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo json_encode(['success' => true, 'hosts' => $hosts], JSON_UNESCAPED_UNICODE);
?>