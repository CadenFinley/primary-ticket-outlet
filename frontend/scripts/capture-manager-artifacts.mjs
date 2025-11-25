import { chromium } from 'playwright'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..', '..')
const screenshotPath = path.resolve(repoRoot, 'docs/proof/manager-ticket-form.png')
const csvPath = path.resolve(repoRoot, 'docs/proof/manager-purchasers.csv')

async function run() {
  const browser = await chromium.launch()
  const page = await browser.newPage()

  await page.goto('http://127.0.0.1:4173/login', { waitUntil: 'networkidle' })
  await page.getByLabel('Email').fill('purple.manager@example.com')
  await page.getByLabel('Display Name').fill('Go Wildcats!')
  await page.getByLabel('Sign in with manager role').check()
  await page
    .getByLabel('Managed Venue IDs (comma separated UUIDs)')
    .fill('11111111-1111-1111-1111-111111111111')
  await page.getByRole('button', { name: /start session/i }).click()
  await page.waitForURL('http://127.0.0.1:4173/')
  await page.getByRole('button', { name: 'Manager' }).click()

  const ticketField = page.getByLabel('Ticket Quantity').first()
  await ticketField.waitFor()
  await ticketField.scrollIntoViewIfNeeded()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: screenshotPath, fullPage: true })

  const downloadPromise = page.waitForEvent('download')
  await page
    .getByRole('button', { name: 'Download Purchasers CSV' })
    .first()
    .click()
  const download = await downloadPromise
  await download.saveAs(csvPath)

  await browser.close()
  console.log('Artifacts saved to:', screenshotPath, csvPath)
}

run().catch((error) => {
  console.error(error)
  process.exit(1)
})
