<?php
header('Content-Type: application/json; charset=utf-8');
include 'db.php';

$data = json_decode(file_get_contents('php://input'), true);
$user_id = $data['user_id'] ?? null;
$name = $data['name'] ?? '';
$address = $data['address'] ?? '';
$os_type = $data['os_type'] ?? 'windows';

if (!$user_id || empty($name) || empty($address)) {
    echo json_encode(['success' => false, 'message' => 'Недостаточно данных']);
    exit;
}

$stmt = $pdo->prepare("INSERT INTO hosts (user_id, name, address, os_type) VALUES (?, ?, ?, ?)");
if ($stmt->execute([$user_id, $name, $address, $os_type])) {
    echo json_encode(['success' => true, 'message' => 'Хост добавлен']);
} else {
    echo json_encode(['success' => false, 'message' => 'Ошибка добавления']);
}
?>