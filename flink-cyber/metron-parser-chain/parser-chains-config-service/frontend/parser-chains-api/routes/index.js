const router = require('express').Router();

const parserconfigRouter = require('./parserconfig');
router.use('/parserconfig', parserconfigRouter);

module.exports = router;