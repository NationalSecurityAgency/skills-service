const fs = require('fs')
const pdf = require('pdf-parse')

const readPdf = (filename) => {
  const dataBuffer = fs.readFileSync(filename)
  return pdf(dataBuffer).then(function (data) {
    return {
      numpages: data.numpages,
      text: data.text,
    }
  })
}

module.exports = { readPdf }