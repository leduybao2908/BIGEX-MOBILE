import express from 'express';
const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 🟢 Định nghĩa trực tiếp route tạo đơn hàng
app.post('/api/create-order', (req, res) => {
  const { amount, userId } = req.body;

  console.log(`Received payment request: amount=${amount}, userId=${userId}`);

  // TODO: Bạn có thể xử lý tạo đơn hàng thực sự ở đây (gọi VNPAY)
  const paymentUrl = 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000';

  res.json({
    success: true,
    paymentUrl: paymentUrl,
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
